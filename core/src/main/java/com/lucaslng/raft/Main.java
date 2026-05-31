package com.lucaslng.raft;

import com.badlogic.gdx.Game;
import com.lucaslng.raft.screen.GameScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create() {
        setScreen(new GameScreen());
    }
}