package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector2;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.BlueprintLearnedEvent;

// A floating blueprint which unlocks the next recipe by posting an event
public class OceanBlueprint extends OceanTrash {

	static private Model blueprintModel;

	static private Model getBlueprintModel(Assets assets) {
		if (blueprintModel == null)
			blueprintModel = assets.get("models/blueprint.g3db", Model.class);
		return blueprintModel;
	}

	public OceanBlueprint(Vector2 position, Vector2 windDir, Player player, Assets assets) {
		super(getBlueprintModel(assets), position, windDir, player);
	}

	@Override
	public void onClick(EventBus events) {
		super.onClick(events);
		events.post(new BlueprintLearnedEvent());
	}
	
}
