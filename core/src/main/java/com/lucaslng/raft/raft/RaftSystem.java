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

/**
 * Manages the grid of {@link RaftTile}s.
 *
 * <h3>Raft drifting</h3>
 * The raft has a world-space origin ({@link #raftPosition}). All tile and
 * building positions are expressed as <em>local</em> grid coordinates; the
 * actual world position is {@code raftPosition + tileLocalCoord}.
 *
 * <p>
 * Calling {@link #drift(Vector2, float)} moves {@code raftPosition} and
 * re-syncs every tile's physics transform. A {@code Sail} building will
 * influence the drift speed multiplier via {@link #setSailMultiplier(float)}
 * and can override the drift direction via {@link #setSailDirection(Vector2)}.
 *
 * <h3>Direction priority</h3>
 * When a sail is placed ({@code sailMultiplier > 0}) the raft drifts along
 * {@link #sailDirection} instead of the wind direction passed to
 * {@link #drift(Vector2, float)}. When no sail is present the raft is
 * stationary (sailMultiplier == 0).
 **/
public class RaftSystem implements Disposable {

	// Cardinal directions — immutable references used for neighbour queries.
	public static final Vector2[] DIRS = {
			new Vector2(1, 0),
			new Vector2(-1, 0),
			new Vector2(0, 1),
			new Vector2(0, -1),
	};

	private final Model tileModel;
	private final PhysicsSystem physics;
	private final EventBus events;

	/** World-space position of the raft's local origin (grid 0,0). */
	private final Vector2 raftPosition = new Vector2(0, 0);

	/** Speed multiplier — 0 = anchored, > 0 = drifting. Set by Sail. */
	private float sailMultiplier = 0f;

	/**
	 * Direction the raft moves when a sail is present.
	 * Starts as a zero vector; set when a sail is placed or steered.
	 * If zero, falls back to whatever direction is passed to {@link #drift}.
	 */
	private final Vector2 sailDirection = new Vector2(0f, 0f);

	private final Map<Vector2, RaftTile> tiles = new HashMap<>();
	private final List<RaftTile> tileList = new ArrayList<>();
	private final List<ModelInstance> instanceCache = new ArrayList<>();
	private boolean instancesDirty = true;

	// Reusable scratch vector — never stored across frames.
	private final Vector2 scratch = new Vector2();

	public RaftSystem(Model tileModel, PhysicsSystem physics, EventBus events) {
		this.tileModel = tileModel;
		this.physics = physics;
		this.events = events;
		addTile(new Vector2(0, 0));
	}

	// ── Tile management ──────────────────────────────────────────────────────

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
			if (hasTile(scratch.set(coord).add(d)))
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

	/**
	 * Uses the camera look direction to find the best adjacent empty grid cell
	 * next to the tile the player is looking at.
	 *
	 * @return local grid coordinate, or {@code null} if no valid expansion exists.
	 */
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

	// ── Drift / movement ────────────────────────────────────────────────────

	/**
	 * Advances the raft each frame.
	 *
	 * <p>
	 * When {@code sailMultiplier > 0} the raft moves along
	 * {@link #sailDirection} (set by the player) at {@code sailMultiplier}
	 * world-units per second, <em>ignoring</em> the supplied {@code windDir}.
	 * When no sail is present ({@code sailMultiplier == 0}) the raft is
	 * stationary.
	 *
	 * @param windDir normalised wind direction (kept for reference only when sail
	 *                active)
	 * @param delta   frame delta in seconds
	 */
	public void drift(Vector2 windDir, float delta) {
		if (sailMultiplier == 0f)
			return;

		// Use the sail direction override; fall back to wind if sail dir is zero
		Vector2 moveDir = (sailDirection.len2() > 0.0001f) ? sailDirection : windDir;

		float speed = sailMultiplier * delta;
		raftPosition.add(moveDir.x * speed, moveDir.y * speed);
		syncTilePositions();
	}

	/**
	 * Called by a {@link com.lucaslng.raft.building.SailBuilding} to set how
	 * fast the raft drifts. 0 = anchored.
	 */
	public void setSailMultiplier(float multiplier) {
		this.sailMultiplier = Math.max(0f, multiplier);
	}

	/**
	 * Called by a {@link com.lucaslng.raft.building.SailBuilding} (on placement
	 * or when the player steers) to override the drift direction.
	 *
	 * @param direction normalised direction in XZ (x = world X, y = world Z).
	 *                  Pass a zero vector to follow wind.
	 */
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

	/** Current sail heading (normalised), or zero if following wind / no sail. */
	public Vector2 getSailDirection() {
		return sailDirection;
	}

	/** World-space origin of the raft grid. */
	public Vector2 getRaftPosition() {
		return raftPosition;
	}

	/**
	 * Converts a local tile coordinate to a world position (x, z plane).
	 */
	public Vector2 toWorldCoord(Vector2 localCoord) {
		return new Vector2(raftPosition).add(localCoord);
	}

	// ── Update ──────────────────────────────────────────────────────────────

	public void update(float delta) {
		for (RaftTile tile : tileList) {
			if (tile.hasBuilding()) {
				tile.getBuilding().update(delta);
			}
		}
	}

	// ── Rendering ───────────────────────────────────────────────────────────

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

	// ── Disposal ────────────────────────────────────────────────────────────

	@Override
	public void dispose() {
		for (RaftTile tile : tileList) {
			physics.removeBody(tile.getBody());
			tile.dispose();
		}
		tiles.clear();
		tileList.clear();
		instanceCache.clear();
	}

	// ── Private helpers ──────────────────────────────────────────────────────

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