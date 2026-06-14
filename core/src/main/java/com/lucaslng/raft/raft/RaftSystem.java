package com.lucaslng.raft.raft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.TilePlacedEvent;
import com.lucaslng.raft.physics.PhysicsSystem;

// Manages grid of RaftTiles
// Drifts in the ocean
public class RaftSystem implements Disposable {

	// cardinal directions
	public static final Vector2[] DIRS = {
			new Vector2(1, 0),
			new Vector2(-1, 0),
			new Vector2(0, 1),
			new Vector2(0, -1),
	};

	private final Model tileModel;
	private final PhysicsSystem physics;
	private final EventBus events;

	private final Vector2 raftPosition = new Vector2(0, 0);

	private float sailMultiplier = 0f;

	private final Vector2 sailDirection = new Vector2(0f, 0f);

	private final Map<Vector2, RaftTile> tiles = new HashMap<>();
	private final List<RaftTile> tileList = new ArrayList<>();
	private final List<ModelInstance> instanceCache = new ArrayList<>();
	private boolean instancesDirty = true;

	// temp vector
	private final Vector2 _scratch = new Vector2();

	public RaftSystem(Model tileModel, PhysicsSystem physics) {
		this.tileModel = tileModel;
		this.physics = physics;
		this.events = EventBus.get();
		addTile(new Vector2(0, 0)); // starting tile
	}

	public boolean hasTile(Vector2 coord) {
		return tiles.containsKey(coord);
	}

	public RaftTile getTile(Vector2 coord) {
		return tiles.get(coord);
	}

	public boolean isValidExpansion(Vector2 coord) {
		if (hasTile(coord))
			return false;
		for (Vector2 d : DIRS)
			if (hasTile(_scratch.set(coord).add(d)))
				return true;
		return false;
	}

	public RaftTile placeTile(Vector2 localCoord) {
		if (!isValidExpansion(localCoord))
			throw new IllegalArgumentException("Invalid tile placement at " + localCoord);
		RaftTile tile = addTile(localCoord);
		instancesDirty = true;
		events.post(new TilePlacedEvent(tile));
		return tile;
	}

	// calculate which direction is closest based on camera
	public Vector2 bestExpansionNeighbour(RaftTile hit, Vector3 lookDir) {
		Vector2 look2d = new Vector2(lookDir.x, lookDir.z);
		Vector2 best = null;
		float bestDot = Float.NEGATIVE_INFINITY;

		for (Vector2 d : DIRS) {
			float dot = d.dot(look2d);
			if (dot > bestDot) {
				bestDot = dot;
				best = new Vector2(hit.coord).add(d);
			}
		}
		return (best != null && isValidExpansion(best)) ? best : null;
	}

	// called every frame to drift the raft
	public void drift(Vector2 windDir, float delta) {
		if (sailMultiplier == 0f)
			return;

		Vector2 moveDir = (sailDirection.len2() > 0.0001f) ? sailDirection : windDir;

		float speed = sailMultiplier * delta;
		raftPosition.add(moveDir.x * speed, moveDir.y * speed);
		syncTilePositions();
	}

	public void setSailMultiplier(float multiplier) {
		this.sailMultiplier = Math.max(0f, multiplier);
	}

	public void setSailDirection(Vector2 direction) {
		if (direction == null || direction.len2() < 0.0001f) {
			sailDirection.set(0f, 0f);
		} else {
			sailDirection.set(direction).nor();
		}
	}

	public float getSailMultiplier() {
		return sailMultiplier;
	}

	public Vector2 getSailDirection() {
		return sailDirection;
	}

	public Vector2 getRaftPosition() {
		return raftPosition;
	}

	// convert raft tile coords to world coords
	public Vector2 toWorldCoord(Vector2 localCoord) {
		return new Vector2(raftPosition).add(localCoord);
	}

	// update every tile
	public void update(float delta) {
		for (RaftTile tile : tileList) {
			if (tile.hasBuilding()) {
				tile.getBuilding().update(delta);
			}
		}
	}

	// rendering

	public List<ModelInstance> getInstances() {
		if (instancesDirty) {
			instanceCache.clear();
			for (RaftTile tile : tileList) {
				instanceCache.add(tile.getInstance());
				if (tile.hasBuilding())
					instanceCache.add(tile.getBuilding().getInstance());
			}
			instancesDirty = false;
		}
		return instanceCache;
	}

	public Iterable<RaftTile> getTiles() {
		return tileList;
	}

	public void markDirty() {
		instancesDirty = true;
	}

	@Override
	public void dispose() {
		for (RaftTile tile : tileList) {
			physics.removeBody(tile.getBody());
			tile.dispose();
		}
	}

	private RaftTile addTile(Vector2 localCoord) {
		Vector2 worldCoord = toWorldCoord(localCoord);
		RaftTile tile = new RaftTile(new Vector2(localCoord), worldCoord, tileModel);
		tiles.put(new Vector2(localCoord), tile);
		tileList.add(tile);
		physics.addBody(tile.getBody());
		instancesDirty = true;
		return tile;
	}

	private void syncTilePositions() {
		for (RaftTile tile : tileList) {
			Vector2 world = toWorldCoord(tile.coord);
			tile.setWorldPosition(world);
		}
	}
}