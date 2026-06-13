package com.lucaslng.raft;

import com.badlogic.gdx.Game;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.screen.LoadingScreen;
import com.lucaslng.raft.screen.ScreenManager;
import com.lucaslng.raft.settings.Settings;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    private Assets assets;
    private ScreenManager screenManager;

    @Override
    public void create() {
        assets = new Assets();
        new Settings();
        screenManager = new ScreenManager(this);
        screenManager.replace(new LoadingScreen(assets, screenManager));
    }

    @Override
    public void dispose() {
        assets.dispose();
        screenManager.dispose();
    }
}