package com.raphfrk.bukkit.eventlink;

import java.io.Serializable;
import java.util.Arrays;

public class EventLinkPacket implements Serializable {

	private static final long serialVersionUID = 1L;

	EventLinkPacket(String sourceServer, String destinationServer, Object payload) {
		this(sourceServer, destinationServer, payload, 10);
	}

	EventLinkPacket(String sourceServer, String[] destinationServers, Object payload) {
		this(sourceServer, destinationServers, payload, 10);
	}
		
	EventLinkPacket(String sourceServer, String destinationServer, Object payload, int timeToLive) {
		this.sourceServer = sourceServer;
		this.destinationServer = new String[1];
		this.destinationServer[0] = destinationServer;
		this.payload = payload;
		this.timeToLive = timeToLive;
	}
	
	EventLinkPacket(String sourceServer, String[] destinationServer, Object payload, int timeToLive) {
		this.sourceServer = sourceServer;
		this.destinationServer = destinationServer;
		this.payload = payload;
		this.timeToLive = timeToLive;
	}
	
	EventLinkPacket(EventLinkPacket eventLinkPacket, String[] destinationServers) {
		this.sourceServer = eventLinkPacket.sourceServer;
		this.destinationServer = destinationServers;
		this.payload = eventLinkPacket.payload;
		this.timeToLive = eventLinkPacket.timeToLive;
	}
	
	final public String sourceServer;
	final public String[] destinationServer;
	final public Object payload;
	int timeToLive = 10;
	
	public String toString() {
		return sourceServer + "->" + Arrays.toString(destinationServer) + " [" + payload + "]";
	}
	
	//NOTE: need to reset connection due to object cache
	
}