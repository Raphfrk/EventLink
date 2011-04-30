package com.raphfrk.bukkit.eventlink;

import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.raphfrk.bukkit.eventlinkapi.EventLinkAPI;

public class EventLinkAPIInterface implements EventLinkAPI {

	EventLink p;
	
	EventLinkAPIInterface(EventLink p) {
		this.p = p;
	}

	public boolean isAdmin(Player player) {
		return player.isOp() || p.admins.contains(player.getName().toLowerCase());
	}

	public boolean sendEvent(String target, Event event) {
		if(p.connectionManager==null) {
			return false;
		}
		return p.connectionManager.sendObject(target, event);
	}

	public boolean sendEvent(String[] target, Event event) {
		if(p.connectionManager==null) {
			return false;
		}
		return p.connectionManager.sendObject(target, event);
	}
	
	public boolean addRouteEntry(String table, String name) {
		return p.routingTableManager.addEntry(table, name);
	}

	public boolean deleteRouteEntry(String table, String name) {
		return p.routingTableManager.deleteEntry(table, name);
	}

	public String getEntryLocation(String table, String name) {
		return p.routingTableManager.getLocation(table, name);

	}

	public Set<String> copyEntries(String table) {
		return p.routingTableManager.copyKeySet(table);
	}

	public String getServerName() {
		return p.serverName;
	}

	public boolean sendMessage(String playerName, String message) {
		return sendMessage(null, playerName, message);
	}

	public boolean sendMessage(String fromPlayer, String toPlayer, String message) {
		return EventLinkMessageEvent.sendMessage(fromPlayer, toPlayer, message, p);
	}
	
}
