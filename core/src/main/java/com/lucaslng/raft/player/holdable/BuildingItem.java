package com.lucaslng.raft.player.holdable;

import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.event.events.BuildingPlacedEvent;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.world.World;

import java.util.Map;

// Holdable item which is building to be placed
public class BuildingItem extends Holdable {

    private final String buildingName;

    public BuildingItem(String buildingName) {
        super();
        this.buildingName = buildingName;
    }

    @Override
    public String getName() {
        return buildingName;
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
        Map<String, Integer> cost = registry.getCost(buildingName);
        if (cost == null) return;

        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (world.getPlayer().getBackpack().getCount(entry.getKey()) < entry.getValue())
                return;
        }

        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            world.getPlayer().getBackpack().consume(entry.getKey(), entry.getValue());
        }

        Building building = registry.create(buildingName);
        tile.setBuilding(building);
        world.getEvents().post(new BuildingPlacedEvent(tile, building));
    }

    @Override
    public void onRightClick() {}
}