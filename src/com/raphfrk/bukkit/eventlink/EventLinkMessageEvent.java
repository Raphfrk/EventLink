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
	
}
