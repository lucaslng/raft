package com.lucaslng.raft.player.holdable;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.lucaslng.raft.assets.Assets;

public class HoldableRegistry {

	private final HashMap<String, TextureRegion> textures;
	
	public HoldableRegistry(Assets assets) {
		textures = new HashMap<>();
		TextureAtlas atlas = assets.get("textures/sprites.atlas", TextureAtlas.class);
		textures.put("Hammer", atlas.findRegion("hammer"));
	}

	public TextureRegion getTexture(String name) {
		return textures.get(name);
	}
}
