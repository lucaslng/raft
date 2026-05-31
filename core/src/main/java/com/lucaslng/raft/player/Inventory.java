package com.lucaslng.raft.player;

import com.lucaslng.raft.item.ItemStack;

public class Inventory {

	private ItemStack[][] grid;
	
	public Inventory() {
		// start with 6 rows and 4 cols
		grid = new ItemStack[6][4];
	}

	public ItemStack getItemStackAt(int row, int col) {
		return grid[row][col];
	}

}
