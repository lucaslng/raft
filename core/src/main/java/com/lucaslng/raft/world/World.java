package com.lucaslng.raft.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestNotMeRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.entity.*;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.physics.PhysicsSystem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.PlacementGhost;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.raft.RaftTile;

public class World implements Disposable {

	private final Vector2 windDir;
	private final EventBus events;
	private final RaftSystem raftSystem;
	private final PlacementGhost placementGhost;
	private final Player player;
	private final Shark shark;
	private final PhysicsSystem physics;
	private final ItemRegistry itemRegistry;
	private final TrashSystem trashSystem;
	private final BuildingRegistry buildingRegistry;

	// Raycast state
	private Entity hoveredEntity;
	private RaftTile hoveredRaftTile;
	private Vector2 ghostTarget;

	private ClosestNotMeRayResultCallback rayResultCallback;

	public World(Assets assets, EventBus events) {
		windDir = new Vector2(1f, .8f).nor();

		this.events = events;

		physics = new PhysicsSystem(events);

		Model tileModel = assets.get("models/platform.g3db", Model.class);
		raftSystem = new RaftSystem(tileModel, physics, events);
		placementGhost = new PlacementGhost(tileModel);

		shark = new Shark(assets.get("models/shark.g3dj", Model.class), new Vector3(3f, 1f, 0f));
		player = new Player(assets.get("models/character-male.g3dj", Model.class), new Vector3(0f, 1f, 0f), events);
		physics.addEntity(player);
		physics.addEntity(shark);

		rayResultCallback = new ClosestNotMeRayResultCallback(player.getBody());

		itemRegistry = new ItemRegistry(assets);
		buildingRegistry = new BuildingRegistry(assets, events);
		trashSystem = new TrashSystem(events, physics, itemRegistry, assets, windDir);

		// Give the player one BuildingItem per registered building at game start.
		// These are distributed via HoldableItemReceivedEvent → Hotbar.
		buildingRegistry.giveStartingItems(events);
	}

	public void update(float delta, Camera camera) {
		player.update(delta);
		shark.update(delta);
		physics.update(delta);
		raftSystem.update(delta);
		trashSystem.update(delta, player.getPosition());

		checkRaycast(camera);
	}

	private void checkRaycast(Camera cam) {
		rayResultCallback.setClosestHitFraction(1f);
		rayResultCallback.setCollisionObject(null);
		hoveredEntity = null;
		hoveredRaftTile = null;
		ghostTarget = null;

		Vector3 rayEnd = cam.direction.cpy().scl(10f).add(cam.position);
		physics.rayCast(cam.position, rayEnd, rayResultCallback);

		if (rayResultCallback.hasHit()) {
			btCollisionObject obj = rayResultCallback.getCollisionObject();
			Object userData = obj.userData;

			if (userData instanceof Entity) {
				hoveredEntity = (Entity) userData;

				if (userData instanceof OceanTrash) {
					OceanTrash t = (OceanTrash) userData;
					if (Gdx.input.isButtonPressed(Buttons.LEFT))
						t.onClicked(events);
				}
			} else if (userData instanceof RaftTile) {
				RaftTile tile = (RaftTile) userData;
				hoveredRaftTile = tile;

				Holdable held = player.getHotbar().getHeldItem();

				if (held instanceof Hammer) {
					ghostTarget = raftSystem.bestExpansionNeighbour(tile, cam.direction);

					if (ghostTarget != null && Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
						held.onLeftClick(this);
					}
				} else if (held instanceof BuildingItem) {
					// Left-click places the building on the hovered tile
					if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
						held.onLeftClick(this);
					}
				} else if (held != null && Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
					held.onLeftClick(this);
				}
			}
		}

		placementGhost.setTarget(ghostTarget);
	}

	// ── Getters ──────────────────────────────────────────────────────────────

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

	public Iterable<OceanTrash> getTrash() {
		return trashSystem.getTrash();
	}

	public PhysicsSystem getPhysics() {
		return physics;
	}

	public Entity getHoveredEntity() {
		return hoveredEntity;
	}

	public RaftTile getHoveredRaftTile() {
		return hoveredRaftTile;
	}

	public Vector2 getGhostTarget() {
		return ghostTarget;
	}

	public BuildingRegistry getBuildingRegistry() {
		return buildingRegistry;
	}

	public EventBus getEvents() {
		return events;
	}

	@Override
	public void dispose() {
		raftSystem.dispose();
		player.dispose();
		shark.dispose();
		physics.dispose();
	}
}