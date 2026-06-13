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

public class RaftSystem implements Disposable {

	private final Model tileModel;
	private final PhysicsSystem physics;
	private final EventBus events;

	private final Map<Vector2, RaftTile> tiles = new HashMap<>();
	private final List<RaftTile> tileList = new ArrayList<>();

	private static final Vector2 tempVec = new Vector2();

	public static final Vector2[] DIRS = {
			new Vector2( 1, 0 ), new Vector2( -1, 0 ), new Vector2( 0, 1 ), new Vector2( 0, -1 )
	};

	public RaftSystem(Model tileModel, PhysicsSystem physics, EventBus events) {
		this.tileModel = tileModel;
		this.physics = physics;
		this.events = events;

		addTile(Vector2.Zero);
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
			if (hasTile(tempVec.set(coord).add(d)))
				return true;
		return false;
	}

	public RaftTile placeTile(Vector2 coord) {
		if (!isValidExpansion(coord))
			throw new IllegalArgumentException();
		RaftTile tile = addTile(coord);
		events.post(new TilePlacedEvent(tile));
		return tile;
	}

	// Use player direction to find selected tile
	public Vector2 bestExpansionNeighbour(RaftTile hit, Vector3 lookDir) {
		tempVec.set(lookDir.x, lookDir.z);
		Vector2 best = new Vector2();
		float bestDot = Float.MIN_VALUE;
		for (Vector2 d : DIRS) {
			float dot = d.dot(tempVec);
			if (dot > bestDot) {
				bestDot = dot;
				best.set(d);
			}
		}
		best.add(hit.coord);
		return isValidExpansion(best) ? best : null;
	}

	public Iterable<RaftTile> getTiles() {
		return tileList;
	}

	public Iterable<ModelInstance> getInstances() {
		List<ModelInstance> out = new ArrayList<>();
		for (RaftTile tile : tileList) {
			out.add(tile.getInstance());
			if (tile.hasBuilding()) {
				out.add(tile.getBuilding().getInstance());
			}
		}
		return out;
	}

	public void update(float delta) {
		for (RaftTile tile : tileList) {
			if (tile.hasBuilding()) {
				tile.getBuilding().update(delta);
			}
		}
	}

	@Override
	public void dispose() {
		for (RaftTile tile : tileList) {
			physics.removeBody(tile.getBody());
			tile.dispose();
		}
		tiles.clear();
		tileList.clear();
	}

	private RaftTile addTile(Vector2 coord) {
		RaftTile tile = new RaftTile(coord, tileModel);
		tiles.put(new Vector2(coord), tile);
		tileList.add(tile);
		physics.addBody(tile.getBody());
		return tile;
	}

}