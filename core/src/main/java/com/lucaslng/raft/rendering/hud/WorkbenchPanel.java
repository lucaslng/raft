package com.lucaslng.raft.rendering.hud;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.lucaslng.raft.crafting.CraftingRecipe;
import com.lucaslng.raft.crafting.CraftingRegistry;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.Hotbar;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.world.World;

// Workbench panel to craft items
public class WorkbenchPanel implements Panel {

	private final EventBus events;

	public WorkbenchPanel(EventBus events) {
		this.events = events;
	}

	@Override
	public void populate(Table table, World world) {
		Skin skin = table.getSkin();
		CraftingRegistry craftingRegistry = world.getCraftingRegistry();
		Backpack backpack = world.getPlayer().getBackpack();
		Hotbar hotbar = world.getPlayer().getHotbar();

		table.add(new Label("Workbench", skin, "title")).padBottom(20f).row();

		Table recipeList = new Table(skin);
		recipeList.left().top();

		ScrollPane scroll = new ScrollPane(recipeList, skin);
		scroll.setFadeScrollBars(false);
		scroll.setScrollingDisabled(false, true); // scroll horizontal only
		scroll.setForceScroll(false, true);

		table.add(scroll).grow().pad(30f).row();

		buildRecipeList(recipeList, skin, craftingRegistry, backpack, hotbar);

		TextButton close = new TextButton("Close", skin);
		close.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				events.post(new PanelOpenedEvent(null));
			}
		});
		table.add(close).width(180f).height(60f).padBottom(100f);
	}

	private void buildRecipeList(Table list, Skin skin,
			CraftingRegistry craftingRegistry, Backpack backpack, Hotbar hotbar) {

		List<CraftingRecipe> unlocked = craftingRegistry.getUnlocked();

		if (unlocked.isEmpty()) {
			list.add(new Label("No recipes unlocked yet.\nCollect blueprints from the ocean!", skin));
			return;
		}

		for (CraftingRecipe recipe : unlocked) {
			Table recipeTable = new Table(skin);
			recipeTable.defaults().left().pad(4f);

			// Recipe name
			recipeTable.add(new Label(recipe.name, skin, "white")).padBottom(8f).row();

			boolean canAfford = true;

			// Show each ingredient with colors
			for (Map.Entry<String, Integer> entry : recipe.ingredients.entrySet()) {
				String itemName = entry.getKey();
				int required = entry.getValue();
				int have = backpack.getCount(itemName);

				boolean enough = have >= required;
				if (!enough)
					canAfford = false;

				String style = enough ? "green" : "red";
				recipeTable.add(new Label(String.format("%s: %d / %d", itemName, have, required), skin, style)).row();
			}

			TextButton craftBtn = new TextButton("Craft", skin);
			craftBtn.setDisabled(!canAfford || hotbar.isFull());

			craftBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Holdable result = craftingRegistry.craft(recipe.name, backpack);
					if (result != null) {
						events.post(new HoldableItemRecievedEvent(result));
						list.clearChildren();
						buildRecipeList(list, skin, craftingRegistry, backpack, hotbar); // rebuild after craft
					}
				}
			});

			recipeTable.add(craftBtn).padTop(8f).growX().row();
			list.add(recipeTable).width(250f).top().pad(30f);
		}
	}
}