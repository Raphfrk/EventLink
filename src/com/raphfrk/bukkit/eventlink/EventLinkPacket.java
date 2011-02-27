package com.raphfrk.bukkit.eventlink;

import java.io.Serializable;

public class EventLinkPacket implements Serializable {

	private static final long serialVersionUID = 1L;

	EventLinkPacket(String sourceServer, String destinationServer, Object payload) {
		
		this(sourceServer, destinationServer, payload, 10);
		
	}

		
	EventLinkPacket(String sourceServer, String destinationServer, Object payload, int timeToLive) {
		this.sourceServer = sourceServer;
		this.destinationServer = destinationServer;
		this.payload = payload;
		this.timeToLive = timeToLive;
	}
	final public String sourceServer;
	final public String destinationServer;
	final public Object payload;
	int timeToLive = 10;
	
	public String toString() {
		
		return sourceServer + "->" + destinationServer + " [" + payload + "]";
		
	}
	
	//NOTE: need to reset connection due to object cache
}