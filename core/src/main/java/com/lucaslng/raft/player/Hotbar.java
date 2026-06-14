package com.lucaslng.raft.player;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.event.events.HotbarIndexEvent;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.player.holdable.Hammer;

public class Hotbar {

  public static final int HOTBAR_SIZE = 8;

  private final Holdable[] hotbar;
  private int heldIndex;

  public Hotbar(EventBus events) {
    hotbar = new Holdable[HOTBAR_SIZE];
    heldIndex = 0;

    // Slot 0 is always the Hammer (tile-placing tool).
    hotbar[0] = new Hammer();

    // Any holdable item received (e.g. BuildingItems crafted at a Workbench)
    // goes into the next free slot starting at index 1.
    events.subscribe(HoldableItemRecievedEvent.class, new Subscriber<HoldableItemRecievedEvent>() {
      @Override
      public void accept(HoldableItemRecievedEvent event) {
        int slot = nextFreeSlot();
        if (slot >= 0)
          hotbar[slot] = event.item;
      }
    });

    events.subscribe(HotbarIndexEvent.class, new Subscriber<HotbarIndexEvent>() {
      @Override
      public void accept(HotbarIndexEvent event) {
        heldIndex = event.index;
      }
    });
  }

  public Holdable getHeldItem() {
    return hotbar[heldIndex];
  }

  public int getHeldIndex() {
    return heldIndex;
  }

  public Holdable getItem(int index) {
    return (index >= 0 && index < HOTBAR_SIZE) ? hotbar[index] : null;
  }

  /**
   * Removes the currently held item from its hotbar slot (nulls it out).
   * Slot 0 (the Hammer) is never removed.
   * Used when a {@link com.lucaslng.raft.player.holdable.BuildingItem} is
   * consumed on placement.
   */
  public void removeHeldItem() {
    if (heldIndex == 0)
      return; // Hammer slot is permanent
    hotbar[heldIndex] = null;
    // Switch to slot 0 (Hammer) so the player isn't left holding nothing.
    heldIndex = 0;
  }

  /**
   * Returns the first free slot (index ≥ 1). Slot 0 is reserved for the Hammer.
   */
  private int nextFreeSlot() {
    for (int i = 1; i < hotbar.length; i++)
      if (hotbar[i] == null)
        return i;
    return -1;
  }

  public boolean isFull() {
    return nextFreeSlot() == -1;
  }
}