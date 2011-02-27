package com.raphfrk.bukkit.eventlink;

import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;

public class EventLinkPlayerListener extends PlayerListener {

	final EventLink p;
	
	EventLinkPlayerListener(EventLink p) {
		this.p = p;
	}
		
    public void onPlayerJoin(PlayerEvent event) {
    	p.routingTableManager.addEntry("players", event.getPlayer().getName());
    }
    
    public void onPlayerQuit(PlayerEvent event) {
    	p.routingTableManager.deleteEntry("players", event.getPlayer().getName());
    }
	
}
