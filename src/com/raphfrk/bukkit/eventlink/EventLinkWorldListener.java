package com.raphfrk.bukkit.eventlink;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

public class EventLinkWorldListener extends WorldListener {

	final EventLink p;
	
	EventLinkWorldListener(EventLink p) {
		this.p = p;
	}
	
	@Override
	public void onWorldLoad(WorldLoadEvent event) {
		p.routingTableManager.addEntry("worlds", event.getWorld().getName());
    }
	
}
