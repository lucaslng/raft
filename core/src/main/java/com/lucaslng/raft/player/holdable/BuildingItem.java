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
 * <p>
 * One {@code BuildingItem} covers every building type — the name string is
 * the discriminator.
 * </p>
 *
 * <h3>Consumed on use</h3>
 * <p>
 * After successfully placing the building, this item removes itself from the
 * player's hotbar. The player must craft a new one at the Workbench to place
 * the same building again.
 * </p>
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

		// Instantiate and place.
		Building building = registry.create(buildingName);
		if (building == null)
			return;

		tile.setBuilding(building);
		world.getRaftSystem().markDirty();
		world.getEvents().post(new BuildingPlacedEvent(tile, building));

		// Consume this item — remove it from the hotbar slot.
		world.getPlayer().getHotbar().removeHeldItem();
	}
}