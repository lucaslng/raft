package com.lucaslng.raft.player.holdable;

import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.event.events.BuildingPlacedEvent;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.world.World;

import java.util.Map;

public class WaterFilterTool extends Holdable {

	public static final String NAME = "Water Filter";

	public WaterFilterTool() {
		super();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void onHeld() {}

	@Override
	public void onUnheld() {}

	@Override
	public void onLeftClick(World world) {
		RaftTile tile = world.getHoveredRaftTile();
		if (tile == null || tile.hasBuilding()) return;

		BuildingRegistry registry = world.getBuildingRegistry();
		Map<String, Integer> cost = registry.getCost(NAME);
		if (cost == null) return;

		// Check all ingredients
		for (Map.Entry<String, Integer> entry : cost.entrySet()) {
			if (world.getPlayer().getBackpack().getCount(entry.getKey()) < entry.getValue())
				return;
		}

		// Consume ingredients
		for (Map.Entry<String, Integer> entry : cost.entrySet()) {
			world.getPlayer().getBackpack().consume(entry.getKey(), entry.getValue());
		}

		// Place building
		Building building = registry.create(NAME);
		tile.setBuilding(building);
		world.getEvents().post(new BuildingPlacedEvent(tile, building));
	}

	@Override
	public void onRightClick() {}
}