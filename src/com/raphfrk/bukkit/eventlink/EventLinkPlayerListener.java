package com.raphfrk.bukkit.eventlink;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventLinkPlayerListener extends PlayerListener {

	final EventLink p;
	
	EventLinkPlayerListener(EventLink p) {
		this.p = p;
	}
		
	@Override
    public void onPlayerJoin(PlayerJoinEvent event) {
    	p.routingTableManager.addEntry("players", event.getPlayer().getName());
    }
    
	@Override
    public void onPlayerQuit(PlayerQuitEvent event) {
    	p.routingTableManager.deleteEntry("players", event.getPlayer().getName());
    }
	
}
