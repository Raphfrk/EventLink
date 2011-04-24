package com.raphfrk.bukkit.eventlink;

import java.io.Serializable;

public class RoutingTableEntry implements Serializable {

	private static final long serialVersionUID = 1L;
	private String nextServer;
	private String location;
	private int hops;
	
	@Override
	public String toString() {
		return "Hops: " + hops + " next: " + nextServer + " location: " + location;
	}

	public void setNextServer(String nextServer) {
		this.nextServer = nextServer;
	}
	
	@Override
	public RoutingTableEntry clone() {
		RoutingTableEntry te = new RoutingTableEntry();
		te.hops = hops;
		te.location = location;
		te.nextServer = nextServer;
		return te;
	}

	String getNextServer() {
		return nextServer;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getLocation() {
		return location;
	}

	public void setHops(int hops) {
		this.hops = hops;
	}

	public int getHops() {
		return hops;
	}

	boolean equal(RoutingTableEntry other) {
		return hops == other.hops && nextServer.equals(other.nextServer) && location.equals(other.location);
	}

}