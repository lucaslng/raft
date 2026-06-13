package com.lucaslng.raft.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Assets extends AssetManager {

	public Assets() {
		super();

		// setup font loader
		setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(getFileHandleResolver()));
		setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(getFileHandleResolver()));
		setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(getFileHandleResolver()));

		// ui skin
		load("skin/golden-ui-skin.json", Skin.class);
		
		// floating items
		load("models/debris-wood.g3db", Model.class);
		load("models/debris-stone.g3db", Model.class);
		load("models/string.g3db", Model.class);
		load("models/blueprint.g3db", Model.class);
		load("models/cauliflower.g3db", Model.class);

		// crosshair
		load("images/crosshair-normal.png", Texture.class);
		
		// fonts
		loadDefaultFont(18, "main18.ttf");
		loadDefaultFont(42, "main42.ttf");
		loadDefaultFont(36, "mainBig.ttf");
		loadDefaultFont(100, "title.ttf");

		// backgrounds
		load("images/2.png", Texture.class);
		load("images/3.png", Texture.class);

		// background music
		load("music/The Pirate's Waltz.mp3", Music.class);

		// entities
		load("models/character-male.g3dj", Model.class);
		load("models/shark.g3dj", Model.class);

		// textures
		load("textures/sprites.atlas", TextureAtlas.class);

		// raft
		load("models/platform.g3db", Model.class);
		load("models/water-filter/water-filter.g3dj", Model.class);
		load("models/sail/sail.g3dj", Model.class);
		load("models/pot.g3db", Model.class);

		// sfx
		load("sfx/tile-placed.mp3", Sound.class);
		load("sfx/building-placed.mp3", Sound.class);
	}

	private void loadDefaultFont(int size, String name) {
		FreeTypeFontLoaderParameter param = new FreeTypeFontLoaderParameter();
		param.fontFileName = "fonts/chinese rocks rg.otf";
		param.fontParameters.size = size;
		param.fontParameters.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
		load(name, BitmapFont.class, param);
	}
}
