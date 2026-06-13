package com.lucaslng.raft.world;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestNotMeRayResultCallback;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.entity.*;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.physics.PhysicsSystem;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.PlacementGhost;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.raft.RaftTile;

/**
 * The game-simulation root: owns all subsystems and ticks them each frame.
 *
 * <h3>Right-click handling</h3>
 * <p>
 * When the player right-clicks a tile that has a building,
 * {@link #handleRightClick()} calls {@link Building#onClicked(EventBus)} on
 * that building, which in turn posts a {@code BuildingClickedEvent}. The HUD
 * subscribes to this event and shows the appropriate building UI panel.
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
	private final TrashSystem trashSystem;

	// ── Convenience references (live inside entitySystem) ────────────────────
	private final Player player;
	private final Shark shark;

	// ── Raycast state (updated every frame in checkRaycast) ──────────────────
	private Clickable hoveredClickable = null; // anything clicked on left/right
	private RaftTile hoveredRaftTile = null; // kept: ghost preview needs it
	private Vector2 ghostTarget = null;

	private final ClosestNotMeRayResultCallback rayCallback;

	private float time = .4f; // 0 - 1 : midnight - midnight
	static public float DAY_LENGTH_SECONDS = 60f;

	public World(Assets assets, EventBus events) {
		this.events = events;
		windDir = new Vector2(1f, .8f).nor();

		physics = new PhysicsSystem(events);
		entitySystem = new EntitySystem(physics);

		// ── Raft ────────────────────────────────────────────────────────────
		Model tileModel = assets.get("models/platform.g3db", Model.class);
		raftSystem = new RaftSystem(tileModel, physics, events);
		placementGhost = new PlacementGhost(tileModel);

		// ── Entities ────────────────────────────────────────────────────────
		player = new Player(
				assets.get("models/character-male.g3dj", Model.class),
				new Vector3(0f, 1f, 0f), events);
		shark = new Shark(
				assets.get("models/shark.g3dj", Model.class),
				new Vector3(3f, 1f, 0f));

		entitySystem.add(player);
		entitySystem.add(shark);

		rayCallback = new ClosestNotMeRayResultCallback(player.getBody());

		// ── Items / buildings ────────────────────────────────────────────────
		// BuildingRegistry now receives raftSystem + windDir so the Sail can
		// steer the raft independently of wind.
		itemRegistry = new ItemRegistry(assets);
		buildingRegistry = new BuildingRegistry(assets, events, raftSystem, windDir);
		trashSystem = new TrashSystem(events, physics, itemRegistry, assets, windDir);

		// Give the player one BuildingItem per registered building.
		for (String name : buildingRegistry.getNames()) {
			events.post(new HoldableItemRecievedEvent(new BuildingItem(name)));
		}
	}

	// ── Per-frame update ─────────────────────────────────────────────────────

	public void update(float delta, Camera camera) {
		time += delta / DAY_LENGTH_SECONDS;
		time %= 1f;
		entitySystem.update(delta);
		physics.update(delta);
		raftSystem.update(delta);
		// Drift is driven by the sail; the wind direction is passed as fallback
		// but only used when no sail direction override is set.
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

		// Single branch: anything Clickable gets stored.
		// RaftTile is also Clickable (right-click opens its building),
		// so this covers entities, buildings, AND tiles in one cast.
		if (userData instanceof Clickable) {
			hoveredClickable = (Clickable) userData;
		}

		// Ghost preview: only meaningful when hovering a tile with the Hammer.
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

	/**
	 * Called by {@code GameScreen} when the left mouse button is just pressed.
	 */
	public void handleLeftClick() {
		Holdable held = player.getHotbar().getHeldItem();
		if (held == null)
			return;

		// Items in the ocean are always collected on left-click,
		// regardless of held tool.
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

	/**
	 * Called by {@code GameScreen} when the right mouse button is just pressed.
	 * If the hovered tile contains a building, opens that building's UI.
	 */
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