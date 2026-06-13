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
 * <p>Calling {@link #drift(Vector2, float)} moves {@code raftPosition} and
 * re-syncs every tile's physics transform. A {@code Sail} building will
 * influence the drift speed multiplier via {@link #setSailMultiplier(float)}.
**/
public class RaftSystem implements Disposable {

	// Cardinal directions — immutable references used for neighbour queries.
	public static final Vector2[] DIRS = {
			new Vector2( 1,  0),
			new Vector2(-1,  0),
			new Vector2( 0,  1),
			new Vector2( 0, -1),
	};

	private final Model tileModel;
	private final PhysicsSystem physics;
	private final EventBus events;

	/** World-space position of the raft's local origin (grid 0,0). */
	private final Vector2 raftPosition = new Vector2(0, 0);

	/** Multiplier applied to the wind drift speed. Sail buildings write to this. */
	private float sailMultiplier = 0f; // 0 = anchored, 1 = full drift

	private final Map<Vector2, RaftTile> tiles     = new HashMap<>();
	private final List<RaftTile>         tileList  = new ArrayList<>();
	// Pre-built instance list, rebuilt only when tiles/buildings change.
	private final List<ModelInstance>    instanceCache = new ArrayList<>();
	private boolean instancesDirty = true;

	// Reusable scratch vector — never stored across frames.
	private final Vector2 scratch = new Vector2();

	public RaftSystem(Model tileModel, PhysicsSystem physics, EventBus events) {
		this.tileModel = tileModel;
		this.physics   = physics;
		this.events    = events;
		addTile(new Vector2(0, 0));
	}

	// ── Tile management ─────────────────────────────────────────────────────

	public boolean hasTile(Vector2 coord) {
		return tiles.containsKey(coord);
	}

	public RaftTile getTile(Vector2 coord) {
		return tiles.get(coord);
	}

	public boolean isValidExpansion(Vector2 coord) {
		if (hasTile(coord)) return false;
		for (Vector2 d : DIRS)
			if (hasTile(scratch.set(coord).add(d))) return true;
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
		Vector2 best   = null;
		float bestDot  = Float.NEGATIVE_INFINITY;

		for (Vector2 d : DIRS) {
			float dot = d.dot(look2d);
			if (dot > bestDot) {
				bestDot = dot;
				// Fresh Vector2 every time — never alias into DIRS.
				best = new Vector2(hit.coord).add(d);
			}
		}
		return (best != null && isValidExpansion(best)) ? best : null;
	}

	// ── Drift / movement ────────────────────────────────────────────────────

	/**
	 * Advances the raft along the wind direction.
	 *
	 * @param windDir normalised wind direction
	 * @param delta   frame delta in seconds
	 */
	public void drift(Vector2 windDir, float delta) {
		if (sailMultiplier == 0f) return;

		float speed = sailMultiplier * delta;
		raftPosition.add(windDir.x * speed, windDir.y * speed);
		syncTilePositions();
	}

	/**
	 * Called by a {@code Sail} building to set how much the wind moves the raft.
	 * 0 = anchored, 1 = full drift speed.
	 */
	public void setSailMultiplier(float multiplier) {
		this.sailMultiplier = Math.max(0f, multiplier);
	}

	public float getSailMultiplier() {
		return sailMultiplier;
	}

	/** World-space origin of the raft grid. */
	public Vector2 getRaftPosition() {
		return raftPosition;
	}

	/**
	 * Converts a local tile coordinate to a world position (x, z plane).
	 * Buildings and ghost tiles should use this when they need world coords.
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

	/** Returns all model instances (tiles + buildings). Rebuilt only when dirty. */
	public List<ModelInstance> getInstances() {
		if (instancesDirty) {
			instanceCache.clear();
			for (RaftTile tile : tileList) {
				instanceCache.add(tile.getInstance());
				if (tile.hasBuilding()) instanceCache.add(tile.getBuilding().getInstance());
			}
			instancesDirty = false;
		}
		return instanceCache;
	}

	public Iterable<RaftTile> getTiles() {
		return tileList;
	}

	/** Must be called whenever a building is placed or removed. */
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

	/** Re-syncs every tile's visual and physics transform to match the current raftPosition. */
	private void syncTilePositions() {
		for (RaftTile tile : tileList) {
			Vector2 world = toWorldCoord(tile.coord);
			tile.setWorldPosition(world);
		}
	}
}