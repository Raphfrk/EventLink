package com.raphfrk.bukkit.eventlink;

import org.bukkit.event.Event;

public class EventLinkMessageEvent extends Event {
	
	private static final long serialVersionUID = 1L;
	private final String message;
	private final String target;
	
	EventLinkMessageEvent(String target, String message) {
		super("EventLinkMessage");
		this.message = message;
		this.target = target;
	}
	
	String getMessage() {
		return message;
	}
	
	String getTarget() {
		return target;
	}
	
	public static boolean sendMessage(String playerName, String message, EventLink p) {
		
		String playerLocation = p.getEntryLocation("players", playerName);
		if(playerLocation != null) {
			p.sendEvent(playerLocation, new EventLinkMessageEvent(playerName, message));
			return true;
		} else {
			return false;
		}
		
	}
	
}
