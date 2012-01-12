/*******************************************************************************
 * Copyright (C) 2012 Raphfrk
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
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
