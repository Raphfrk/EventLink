package com.raphfrk.bukkit.eventlink;

import org.bukkit.event.Event;

public class EventLinkMessageEvent extends Event {
	
	private static final long serialVersionUID = 1L;
	private final String message;
	private final String target;
	private final String from;
	
	EventLinkMessageEvent(String target, String message) {
		super("EventLinkMessage");
		this.message = message;
		this.target = target;
		this.from = null;
	}
	
	EventLinkMessageEvent(String from, String target, String message) {
		super("EventLinkMessage");
		this.message = message;
		this.target = target;
		this.from = from;
	}
	
	String getMessage() {
		return message;
	}
	
	String getTarget() {
		return target;
	}
	
	String getFrom() {
		return from;
	}
	
	public static boolean sendMessage(String fromPlayer, String playerName, String message, EventLink p) {
		
		String playerLocation = p.eventLinkAPIInterface.getEntryLocation("players", playerName);
		if(playerLocation != null) {
			p.eventLinkAPIInterface.sendEvent(playerLocation, new EventLinkMessageEvent(fromPlayer, playerName, message));
			return true;
		} else {
			return false;
		}
		
	}
	
	public static boolean sendMessage(String playerName, String message, EventLink p) {
		return sendMessage(null, playerName, p);		
	}
	
}
