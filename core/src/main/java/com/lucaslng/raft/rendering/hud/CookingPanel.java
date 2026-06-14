package com.lucaslng.raft.rendering.hud;

import java.util.Map.Entry;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.lucaslng.raft.building.CookingPot;
import com.lucaslng.raft.building.CookingPot.CookingState;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.item.Item;
import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.world.World;

// Panel for cooking pot building
// Refreshes when CookingState changes
public class CookingPanel implements Panel {

	private final CookingPot cookingPot;
	private final EventBus events;

	private CookingState lastState;

	public CookingPanel(CookingPot cookingPot, EventBus events) {
		this.cookingPot = cookingPot;
		this.events = events;
	}

	@Override
	public void populate(Table table, World world) {
		Skin skin = table.getSkin();
		Backpack backpack = world.getPlayer().getBackpack();

		Table child = new Table(skin);
		table.add(child).grow().row();
		child.setFillParent(true);
		child.defaults().pad(4f);

		TextButton close = new TextButton("Close", skin);
		close.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				events.post(new PanelOpenedEvent(null));
			}
		});
		table.add(close).width(180f).height(60f).padBottom(180f);

		ProgressBar bar = new ProgressBar(0f, 1f, .001f, false, skin);
		bar.addAction(new Action() {
			@Override
			public boolean act(float delta) {
				if (cookingPot.getState() == CookingState.COOKING)
					bar.setValue(cookingPot.getProgress());
				return false;
			}
		});

		TextButton eat = new TextButton("Eat", skin);
		eat.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				cookingPot.collect();
			}
		});

		child.addAction(new Action() {
			@Override
			public boolean act(float delta) {
				if (cookingPot.getState() == lastState)
					return false;
				lastState = cookingPot.getState();

				child.clearChildren(false);

				if (cookingPot.getState() == CookingState.EMPTY) {
					// List out cookable items that you have
					// Currently only Cauliflower is cookable, so this loop can only run once
					for (Entry<Item, Integer> e : backpack.getSortedBackpackView()) {
						Item item = e.getKey();
						if (!CookingPot.isCookable(item))
							continue;

						int quantity = e.getValue();
						if (quantity <= 0)
							continue;

						String s = String.format("%s x%d", item.name, quantity);
						child.add(new Label(s, skin));
						TextButton button = new TextButton("Cook", skin);
						button.addListener(new ChangeListener() {
							@Override
							public void changed(ChangeEvent event, Actor actor) {
								backpack.consume(item.name, 1);
								cookingPot.cook(item);
							}
						});
						child.add(button).padLeft(8).width(140f).row();
					}
				} else if (cookingPot.getState() == CookingState.COOKING) {
					child.add(new Label("Cooking " + cookingPot.getCurrentItem().name, skin)).row();
					child.add(bar).width(500f).padTop(10).width(300f).row();
				} else {
					child.add(new Label("Cooked " + cookingPot.getCurrentItem().name, skin)).row();
					child.add(eat).width(140f).row();
				}
				return false;
			}
		});
	}
}