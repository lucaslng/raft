package com.lucaslng.raft.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g3d.Model;

public class Assets extends AssetManager {

	private Map<String, FreeTypeFontGenerator> fontGenerators;

	public Assets() {
		super();
		load("colormap.png", Texture.class);
		load("platform.g3db", Model.class);
		load("debris-wood.g3db", Model.class);

		load("images/crosshair-normal.png", Texture.class);

		fontGenerators = new HashMap<>();
		fontGenerators.put("main", new FreeTypeFontGenerator(Gdx.files.internal("fonts/chinese rocks rg.otf")));
	}

	public BitmapFont generateFont(String font, FreeTypeFontParameter parameter) {
		return fontGenerators.get(font).generateFont(parameter);
	}

	public void dispose() {
		super.dispose();
		for (FreeTypeFontGenerator fontGenerator : fontGenerators.values())
			fontGenerator.dispose();
	}
}
