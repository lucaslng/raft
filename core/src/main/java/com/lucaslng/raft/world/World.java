package com.lucaslng.raft.world;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestNotMeRayResultCallback;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.crafting.CraftingRegistry;
import com.lucaslng.raft.entity.*;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.BlueprintLearnedEvent;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.event.events.WinEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.physics.PhysicsSystem;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.PlacementGhost;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.settings.Settings;

// Owns all game logic and updates it every frame
public class World implements Disposable {

	private final Vector2 windDir = new Vector2(1f, .8f).nor();

	public static final float WIN_DISTANCE = 5000f;

	private final EventBus events;
	private final PhysicsSystem physics;
	private final EntitySystem entitySystem;
	private final RaftSystem raftSystem;
	private final PlacementGhost placementGhost;
	private final ItemRegistry itemRegistry;
	private final BuildingRegistry buildingRegistry;
	private final CraftingRegistry craftingRegistry;
	private final TrashSystem trashSystem;

	private final Player player;
	private final Shark shark;

	// Raycast state
	private Clickable hoveredClickable = null;
	private RaftTile hoveredRaftTile = null;
	private Vector2 ghostTarget = null;

	private final ClosestNotMeRayResultCallback rayCallback;

	private float time = .4f; // 0 - 1 : midnight - midnight
	static public final float DAY_LENGTH_SECONDS = 120f; // 2 minutes per day/night cycle

	public World() {
		events = EventBus.get();
		Assets assets = Assets.get();
		Settings settings = Settings.get();

		physics = new PhysicsSystem();
		entitySystem = new EntitySystem(physics);

		// raft
		Model tileModel = assets.get("models/platform.g3db", Model.class);
		raftSystem = new RaftSystem(tileModel, physics);
		placementGhost = new PlacementGhost(tileModel);

		craftingRegistry = new CraftingRegistry();
		itemRegistry = new ItemRegistry();
		buildingRegistry = new BuildingRegistry(this, craftingRegistry);

		// entities
		player = new Player(
				assets.get("models/character-male.g3dj", Model.class),
				new Vector3(0f, 1f, 0f), events, craftingRegistry);

		Model sharkModel = assets.get("models/shark.g3dj", Model.class);
		for (Material m : sharkModel.materials) {
			m.set(IntAttribute.createCullFace(0));
		}
		// spawn shark underwater
		shark = new Shark(sharkModel, new Vector3(6f, -4f, 0f));

		entitySystem.add(player);
		entitySystem.add(shark);

		rayCallback = new ClosestNotMeRayResultCallback(player.getBody());

		trashSystem = new TrashSystem(events, physics, itemRegistry, assets, windDir);

		// start with workbench and some wood
		events.post(new HoldableItemRecievedEvent(new BuildingItem("Workbench")));
		player.getBackpack().add(itemRegistry.getItem("Wood"), 10);

		// cheats
		if (settings.cheats) {
			player.getBackpack().add(itemRegistry.getItem("Cauliflower"), 100);
			player.getBackpack().add(itemRegistry.getItem("Wood"), 200);
			player.getBackpack().add(itemRegistry.getItem("String"), 300);
			player.getBackpack().add(itemRegistry.getItem("Stone"), 400);
			events.post(new BlueprintLearnedEvent());
			events.post(new BlueprintLearnedEvent());
			events.post(new BlueprintLearnedEvent());
			events.post(new BlueprintLearnedEvent());
		}
	}

	public void update(float delta, Camera camera) {
		time += delta / DAY_LENGTH_SECONDS;
		time %= 1f;

		entitySystem.update(delta);

		shark.update(delta, player.getPosition(), raftSystem.getRaftPosition());

		physics.update(delta);
		raftSystem.update(delta);
		raftSystem.drift(windDir, delta);
		trashSystem.update(delta, player);
		checkRaycast(camera);

		if (player.getPosition().z >= WIN_DISTANCE) {
			events.post(new WinEvent());
		}
	}

	// called every frame
	private void checkRaycast(Camera cam) {
		rayCallback.setClosestHitFraction(1f);
		rayCallback.setCollisionObject(null);
		hoveredClickable = null;
		hoveredRaftTile = null;
		ghostTarget = null;

		Vector3 rayEnd = cam.direction.cpy().scl(10f).add(cam.position);
		physics.rayCast(cam.position, rayEnd, rayCallback);

		if (!rayCallback.hasHit()) {
			placementGhost.setTarget(null);
			return;
		}

		// objects store themselves in userData
		Object userData = rayCallback.getCollisionObject().userData;

		if (userData instanceof Clickable) {
			hoveredClickable = (Clickable) userData;
		}

		if (userData instanceof RaftTile) {
			hoveredRaftTile = (RaftTile) userData;
			Holdable held = player.getHotbar().getHeldItem();
			if (held instanceof Hammer) {
				ghostTarget = raftSystem.bestExpansionNeighbour(
						hoveredRaftTile, cam.direction);
			}
		}

		placementGhost.setTarget(ghostTarget);
	}

	public void handleLeftClick() {
		Holdable held = player.getHotbar().getHeldItem();
		if (held == null)
			return;

		if (hoveredClickable instanceof OceanTrash) {
			hoveredClickable.onClick(events);
			return;
		}

		if (held instanceof Hammer && ghostTarget != null) {
			held.onLeftClick(this);
		} else if (held instanceof BuildingItem && hoveredRaftTile != null) {
			held.onLeftClick(this);
		} else if (hoveredRaftTile != null) {
			held.onLeftClick(this);
		}
	}

	public void handleRightClick() {
		if (hoveredClickable != null) {
			hoveredClickable.onClick(events);
		}
	}

	// Getters

	public float getTime() {
		return time;
	}

	public RaftSystem getRaftSystem() {
		return raftSystem;
	}

	public PlacementGhost getPlacementGhost() {
		return placementGhost;
	}

	public Player getPlayer() {
		return player;
	}

	public Shark getShark() {
		return shark;
	}

	public EntitySystem getEntitySystem() {
		return entitySystem;
	}

	public PhysicsSystem getPhysics() {
		return physics;
	}

	public EventBus getEvents() {
		return events;
	}

	public BuildingRegistry getBuildingRegistry() {
		return buildingRegistry;
	}

	public CraftingRegistry getCraftingRegistry() {
		return craftingRegistry;
	}

	public Vector2 getWindDir() {
		return windDir;
	}

	public Iterable<OceanTrash> getTrash() {
		return trashSystem.getTrash();
	}

	public ModelInstance getHoveredOutlineInstance() {
		if (hoveredClickable instanceof Outlineable) {
			return ((Outlineable) hoveredClickable).getOutlineInstance();
		}
		return null;
	}

	public RaftTile getHoveredRaftTile() {
		return hoveredRaftTile;
	}

	public Clickable getHoveredClickable() {
		return hoveredClickable;
	}

	public Vector2 getGhostTarget() {
		return ghostTarget;
	}

	@Override
	public void dispose() {
		entitySystem.dispose();
		raftSystem.dispose();
		physics.dispose();
		rayCallback.dispose();
	}
}