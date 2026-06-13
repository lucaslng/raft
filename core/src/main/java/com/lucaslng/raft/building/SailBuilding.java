package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.lucaslng.raft.raft.RaftSystem;

/**
 * A sail that controls how much the raft drifts with the wind.
 *
 * <p>When placed, the sail registers itself with the {@link RaftSystem} and
 * increases the drift multiplier. When removed (disposed), the multiplier is
 * reset to zero (anchored).
 *
 * <h3>Integration</h3>
 * Register in {@link BuildingRegistry}:
 * <pre>
 *   register("Sail", () -> new SailBuilding(sailModel, raftSystem),
 *            Map.of("String", 6, "Wood", 8));
 * </pre>
 *
 * <h3>Future work</h3>
 * <ul>
 *   <li>Right-click or interact key could toggle the sail open/closed.</li>
 *   <li>Multiple sails stack the multiplier (capped at some maximum).</li>
 *   <li>Sail direction could be controlled to steer the raft.</li>
 * </ul>
 */
public class SailBuilding extends Building {

	public static final String NAME            = "Sail";
	public static final float  DRIFT_MULTIPLIER = 2f; // world units per second

	private final RaftSystem raftSystem;

	public SailBuilding(Model model, RaftSystem raftSystem) {
		super(new ModelInstance(model));
		this.raftSystem = raftSystem;
	}

	@Override
	public void update(float delta) {
		// No per-tick logic yet. Future: animate sail cloth, react to wind gusts.
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Called by {@link com.lucaslng.raft.raft.RaftTile#setBuilding(Building)}
	 * immediately after placement. Enables raft drift.
	 */
	@Override
	public void setPosition(com.badlogic.gdx.math.Vector3 worldPos) {
		super.setPosition(worldPos);
		raftSystem.setSailMultiplier(DRIFT_MULTIPLIER);
	}

	@Override
	protected void doDispose() {
		// When the sail is destroyed the raft stops drifting.
		raftSystem.setSailMultiplier(0f);
	}
}