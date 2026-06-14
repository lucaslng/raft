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
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.physics.PhysicsSystem;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.PlacementGhost;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.settings.Settings;

/**
 * The game-simulation root: owns all subsystems and ticks them each frame.
 *
 * <h3>Crafting system</h3>
 * <p>
 * A {@link CraftingRegistry} is created here and passed to both
 * {@link BuildingRegistry} (which registers all craftable-building recipes in
 * unlock order) and the {@link com.lucaslng.raft.player.PlayerBlueprints}
 * system (via {@link Player}) which calls
 * {@link CraftingRegistry#onBlueprintCollected()} each time the player picks
 * up an ocean blueprint.
 * </p>
 *
 * <h3>Starting inventory</h3>
 * <p>
 * The player begins with a single {@link BuildingItem} for the Workbench.
 * All other buildings must be crafted at the Workbench once the appropriate
 * blueprints have been collected.
 * </p>
 *
 * <h3>Shark AI</h3>
 * <p>
 * Each frame the shark is driven by its own 3-arg
 * {@link Shark#update(float, Vector3, Vector2)} which receives the current
 * player position and the raft's world-space XZ origin so the shark knows
 * where to lurk.
 * </p>
 */
public class World implements Disposable {

	// ── Wind / environment ───────────────────────────────────────────────────
	private final Vector2 windDir;

	// ── Core systems ─────────────────────────────────────────────────────────
	private final EventBus events;
	private final PhysicsSystem physics;
	private final EntitySystem entitySystem;
	private final RaftSystem raftSystem;
	private final PlacementGhost placementGhost;
	private final ItemRegistry itemRegistry;
	private final BuildingRegistry buildingRegistry;
	private final CraftingRegistry craftingRegistry;
	private final TrashSystem trashSystem;

	// ── Convenience references (live inside entitySystem) ────────────────────
	private final Player player;
	private final Shark shark;

	// ── Raycast state (updated every frame in checkRaycast) ──────────────────
	private Clickable hoveredClickable = null;
	private RaftTile hoveredRaftTile = null;
	private Vector2 ghostTarget = null;

	private final ClosestNotMeRayResultCallback rayCallback;

	private float time = .4f; // 0 - 1 : midnight - midnight
	static public float DAY_LENGTH_SECONDS = 60f;

	public World() {
		events = EventBus.get();
		Assets assets = Assets.get();
		Settings settings = Settings.get();

		windDir = new Vector2(1f, .8f).nor();

		physics = new PhysicsSystem();
		entitySystem = new EntitySystem(physics);

		// ── Raft ────────────────────────────────────────────────────────────
		Model tileModel = assets.get("models/platform.g3db", Model.class);
		raftSystem = new RaftSystem(tileModel, physics);
		placementGhost = new PlacementGhost(tileModel);

		// ── Crafting system ──────────────────────────────────────────────────
		craftingRegistry = new CraftingRegistry();

		// ── Items / buildings ────────────────────────────────────────────────
		itemRegistry = new ItemRegistry(assets);
		buildingRegistry = new BuildingRegistry(assets, events, this, craftingRegistry);

		// ── Entities ────────────────────────────────────────────────────────
		player = new Player(
				assets.get("models/character-male.g3dj", Model.class),
				new Vector3(0f, 1f, 0f), events, craftingRegistry);

		Model sharkModel = assets.get("models/shark.g3dj", Model.class);
		for (Material m : sharkModel.materials) {
			m.set(IntAttribute.createCullFace(0));
		}
		// Spawn the shark underwater below the initial raft origin.
		shark = new Shark(sharkModel, new Vector3(6f, -4f, 0f));

		entitySystem.add(player);
		entitySystem.add(shark);

		rayCallback = new ClosestNotMeRayResultCallback(player.getBody());

		trashSystem = new TrashSystem(events, physics, itemRegistry, assets, windDir);

		// ── Starting inventory ───────────────────────────────────────────────
		events.post(new HoldableItemRecievedEvent(new BuildingItem("Workbench")));

		if (settings.debug) {
			player.getBackpack().add(itemRegistry.get("Cauliflower"), 100);
			player.getBackpack().add(itemRegistry.get("Wood"), 100);
			player.getBackpack().add(itemRegistry.get("String"), 100);
			player.getBackpack().add(itemRegistry.get("Stone"), 100);
			events.post(new BlueprintLearnedEvent());
			events.post(new BlueprintLearnedEvent());
		}
	}

	// ── Per-frame update ─────────────────────────────────────────────────────

	public void update(float delta, Camera camera) {
		time += delta / DAY_LENGTH_SECONDS;
		time %= 1f;

		entitySystem.update(delta);

		// Drive shark AI manually — needs player position + raft origin.
		shark.update(delta, player.getPosition(), raftSystem.getRaftPosition());

		physics.update(delta);
		raftSystem.update(delta);
		raftSystem.drift(windDir, delta);
		trashSystem.update(delta, player.getPosition());
		checkRaycast(camera);
	}

	// ── Raycast ─────────────────────────────────────────────────────────────

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

	// ── Getters ──────────────────────────────────────────────────────────────

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

	// ── Disposal ─────────────────────────────────────────────────────────────

	@Override
	public void dispose() {
		entitySystem.dispose();
		raftSystem.dispose();
		physics.dispose();
	}
}