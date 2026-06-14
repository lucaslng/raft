// Lucas Leung
// Sunday, June 15th, 2026
// Survive the harsh seas on your little raft!
// Collect trash, build structures, and manage your hunger and thirst. Build a sail to control your raft. Find the rumoured safe haven far up north, but be careful of the Sharks!
// Built using libGDX

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
        screenManager.replace(new LoadingScreen());
    }

    @Override
    public void dispose() {
        assets.dispose();
        screenManager.dispose();
    }
}