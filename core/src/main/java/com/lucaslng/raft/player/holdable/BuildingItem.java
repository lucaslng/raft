package com.lucaslng.raft.player.holdable;

import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.event.events.BuildingPlacedEvent;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.world.World;

// Holdable that represents a building that can be placed on the raft
public class BuildingItem extends Holdable {

	private final String buildingName;

	public BuildingItem(String buildingName) {
		this.buildingName = buildingName;
	}

	@Override
	public String getName() {
		return buildingName;
	}

	@Override
	public void onHeld() {
	}

	@Override
	public void onUnheld() {
	}

	@Override
	public void onRightClick() {
	}

	@Override
	public void onLeftClick(World world) {
		RaftTile tile = world.getHoveredRaftTile();
		if (tile == null || tile.hasBuilding())
			return;

		BuildingRegistry registry = world.getBuildingRegistry();

		// Create and place
		Building building = registry.create(buildingName);
		if (building == null)
			return;

		tile.setBuilding(building);
		world.getRaftSystem().markDirty();
		world.getEvents().post(new BuildingPlacedEvent(tile, building));

		// remove from hotbar
		world.getPlayer().getHotbar().removeHeldItem();
	}
}