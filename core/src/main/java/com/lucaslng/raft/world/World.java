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
import com.lucaslng.raft.entity.*;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.physics.PhysicsSystem;
import com.lucaslng.raft.raft.Raft;

public class World implements Disposable {

	private final Vector2 windDir;

	private final EventBus events;
	private final Raft raft;
	private final Player player;
	private final Shark shark;
	private final PhysicsSystem physics;
	private final ItemRegistry itemRegistry;
	private final TrashSystem trashSystem;

	private Entity hoveredEntity;

	private ClosestNotMeRayResultCallback rayResultCallback;
	public World(Assets assets) {
		windDir = new Vector2(1f, .8f).nor();

		events = new EventBus();
		raft = new Raft(assets);
		shark = new Shark(assets.get("models/shark.g3dj", Model.class), new Vector3(3f, 1f, 0f));
		player = new Player(assets.get("models/character-male.g3dj", Model.class), new Vector3(0f, 1f, 0f), events);
		physics = new PhysicsSystem(events);
		physics.addBody(raft.getBody());
		physics.addEntity(player);
		physics.addEntity(shark);

		rayResultCallback = new ClosestNotMeRayResultCallback(player.getBody());


		itemRegistry = new ItemRegistry(assets);
		trashSystem = new TrashSystem(events, physics, itemRegistry, windDir);
	}

	public void update(float delta, Camera camera) {
		raft.update(delta);
		player.update(delta);
		shark.update(delta);
		physics.update(delta);
		trashSystem.update(delta, player.getPosition());

		checkRaycast(camera);
	}

	private void checkRaycast(Camera cam) {
		// reset callback
		rayResultCallback.setClosestHitFraction(1f);
		rayResultCallback.setCollisionObject(null);
		hoveredEntity = null;

		physics.rayCast(cam.position, cam.direction.cpy().scl(100f).add(cam.position), rayResultCallback);
		if (rayResultCallback.hasHit()) {
			// System.out.println("hit");
			btCollisionObject obj = rayResultCallback.getCollisionObject();
			if (obj.userData instanceof OceanTrash) {
				OceanTrash t = (OceanTrash) obj.userData;
				if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
					events.post(new TrashCollectedEvent(t));
				}
				hoveredEntity = t;
			}
		}
	}

	public Raft getRaft() {
		return raft;
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

	@Override
	public void dispose() {
		raft.dispose();
		player.dispose();
		shark.dispose();
		physics.dispose();
	}

}
