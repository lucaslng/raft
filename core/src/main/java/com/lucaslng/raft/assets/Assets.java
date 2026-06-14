package com.lucaslng.raft.assets;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.util.Util;

// Loads game assets asynchronously. Singleton.
public class Assets extends AssetManager {

	private static Assets instance;
	private final List<Disposable> disposables = new ArrayList<>();

	private Skin skin;

	public Assets() {
		super();

		instance = this;

		// setup font loader
		setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(getFileHandleResolver()));
		setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(getFileHandleResolver()));
		setLoader(BitmapFont.class, ".otf", new FreetypeFontLoader(getFileHandleResolver()));

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
		loadDefaultFont(64, "main64.ttf");

		// background music
		load("music/The Pirate's Waltz.mp3", Music.class);

		// entities
		load("models/character-male.g3dj", Model.class);
		load("models/shark.g3dj", Model.class);

		// raft
		load("models/platform.g3db", Model.class);
		load("models/water-filter/water-filter.g3dj", Model.class);
		load("models/sail/sail.g3dj", Model.class);
		load("models/pot.g3db", Model.class);
		load("models/workbench.g3db", Model.class);
		load("models/farm/farm.g3dj", Model.class);

		// sfx
		load("sfx/tile-placed.mp3", Sound.class);
		load("sfx/building-placed.mp3", Sound.class);
		load("sfx/damaged.mp3", Sound.class);
		load("sfx/drink.mp3", Sound.class);
		load("sfx/eat.mp3", Sound.class);
		load("sfx/holdable-recieved.mp3", Sound.class);
		load("sfx/trash-collected.mp3", Sound.class);
	}

	// Load the default font with a specific size
	private void loadDefaultFont(int size, String name) {
		FreeTypeFontLoaderParameter param = new FreeTypeFontLoaderParameter();
		param.fontFileName = "fonts/chinese rocks rg.otf";
		param.fontParameters.size = size;
		param.fontParameters.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
		load(name, BitmapFont.class, param);
	}

	// Create the default skin used for uis
	private Skin createSkin() {
		Skin skin = new Skin();
		BitmapFont font = get("main42.ttf", BitmapFont.class);
		LabelStyle whiteStyle = new LabelStyle(font, Color.WHITE);
		LabelStyle greenStyle = new LabelStyle(font, new Color(0.4f, 1f, 0.4f, 1f));
		LabelStyle redStyle = new LabelStyle(font, new Color(1f, 0.4f, 0.4f, 1f));
		LabelStyle yellowStyle = new LabelStyle(font, new Color(0.9f, 0.95f, 0.5f, 1f));
		LabelStyle titleStyle = new LabelStyle(get("main64.ttf", BitmapFont.class), Color.WHITE);

		Texture buttonUp = Util.generateTexture(
				new Color(0.25f, 0.25f, 0.25f, 1f), 4);

		Texture buttonOver = Util.generateTexture(new Color(0.20f, 0.20f, 0.20f, 1f), 4);

		Texture buttonDown = Util.generateTexture(
				new Color(0.15f, 0.15f, 0.15f, 1f), 4);

		Texture sliderBg = Util.generateTexture(
				new Color(0.3f, 0.3f, 0.3f, 1f), 4);

		Texture sliderKnob = Util.generateTexture(
				Color.WHITE, 16);

		Texture barFg = Util.generateTexture(new Color(0.3f, 0.3f, 0.3f, 1f), 16);
		Texture barBg = Util.generateTexture(Color.WHITE, 16);

		Texture bg = Util.generateTexture(new Color(0.12f, 0.12f, 0.12f, 1f));

		disposables.add(buttonUp);
		disposables.add(buttonOver);
		disposables.add(buttonDown);
		disposables.add(sliderBg);
		disposables.add(sliderKnob);
		disposables.add(barBg);
		disposables.add(barFg);
		disposables.add(bg);

		TextButtonStyle buttonStyle = new TextButtonStyle();
		buttonStyle.up = new TextureRegionDrawable(buttonUp);
		buttonStyle.over = new TextureRegionDrawable(buttonOver);
		buttonStyle.down = new TextureRegionDrawable(buttonDown);
		buttonStyle.disabled = buttonStyle.down;
		buttonStyle.font = font;

		SliderStyle sliderStyle = new SliderStyle();
		sliderStyle.background = new TextureRegionDrawable(sliderBg);
		sliderStyle.knob = new TextureRegionDrawable(sliderKnob);

		ProgressBarStyle barStyle = new ProgressBarStyle();
		barStyle.background = new TextureRegionDrawable(barBg);
		barStyle.knobBefore = new TextureRegionDrawable(barFg);

		ScrollPaneStyle scrollPaneStyle = new ScrollPaneStyle();
		scrollPaneStyle.background = null;

		CheckBoxStyle checkBoxStyle = new CheckBoxStyle();
		checkBoxStyle.font = font;

		skin.add("white", whiteStyle);
		skin.add("default", whiteStyle);
		skin.add("green", greenStyle);
		skin.add("red", redStyle);
		skin.add("yellow", yellowStyle);
		skin.add("title", titleStyle);
		skin.add("default", buttonStyle);
		skin.add("default-horizontal", sliderStyle);
		skin.add("default-horizontal", barStyle);
		skin.add("default", scrollPaneStyle);
		skin.add("default", checkBoxStyle);
		skin.add("bg", bg);
		return skin;
	}

	public Skin getSkin() {
		if (skin == null)
			skin = createSkin();
		return skin;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (skin != null) skin.dispose();
		for (Disposable d : disposables) {
			d.dispose();
		}
	}

	public static Assets get() {
		return instance;
	}
}
