package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.rendering.hud.Panel;

public class PanelOpenedEvent extends Event {

	public final Panel panel;

	public PanelOpenedEvent(Panel panel) {
		this.panel = panel;
	}
	
}
