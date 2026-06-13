package com.lucaslng.raft.player.holdable;

import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.event.events.BuildingPlacedEvent;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.world.World;

import java.util.Map;

/**
 * A holdable that represents a specific building to be placed on the raft.
 *
 * <p>One {@code BuildingItem} covers every building type — the name string is
 * the discriminator.
 */
public class BuildingItem extends Holdable {

	private final String buildingName;

	public BuildingItem(String buildingName) {
		this.buildingName = buildingName;
	}

	@Override
	public String getName() {
		return buildingName;
	}

	@Override public void onHeld()    {}
	@Override public void onUnheld()  {}
	@Override public void onRightClick() {}

	@Override
	public void onLeftClick(World world) {
		RaftTile tile = world.getHoveredRaftTile();
		if (tile == null || tile.hasBuilding()) return;

		BuildingRegistry registry = world.getBuildingRegistry();
		Map<String, Integer> cost = registry.getCost(buildingName);
		if (cost == null) return;

		// Check affordability.
		for (Map.Entry<String, Integer> e : cost.entrySet()) {
			if (world.getPlayer().getBackpack().getCount(e.getKey()) < e.getValue()) return;
		}

		// Consume resources.
		for (Map.Entry<String, Integer> e : cost.entrySet()) {
			world.getPlayer().getBackpack().consume(e.getKey(), e.getValue());
		}

		// Instantiate and place.
		Building building = registry.create(buildingName);
		tile.setBuilding(building);
		world.getRaftSystem().markDirty();
		world.getEvents().post(new BuildingPlacedEvent(tile, building));
	}
}