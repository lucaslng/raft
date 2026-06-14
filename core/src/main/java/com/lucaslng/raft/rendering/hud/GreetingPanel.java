package com.lucaslng.raft.rendering.hud;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.world.World;

// Panel that opens at the very start of the game
// Simply shows a title, description, and close button
public class GreetingPanel implements Panel {

	@Override
	public void populate(Table table, World world) {
		Skin skin = table.getSkin();

		Label title = new Label("Journal", skin, "title");
		table.add(title).padBottom(20f).row();

		Label label = new Label("A great flood has covered the whole world in water. All I have now is my raft and this old workbench. I should place my workbench down so I can craft things.\n\nI recall a safe haven somewhere up north. Maybe I should try to find it...", skin);
		label.setWrap(true);
		table.add(label).center().growX().padLeft(100f).padRight(100f).row();

		TextButton close = new TextButton("Close", skin);
		close.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				EventBus.get().post(new PanelOpenedEvent(null));
			}
		});
		table.add(close).width(180f).height(60f).padBottom(100f).row();
	}

}
