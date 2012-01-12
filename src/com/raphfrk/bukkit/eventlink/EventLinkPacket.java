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
import java.util.Arrays;
import java.util.Random;

public class EventLinkPacket implements Serializable {
	
	final static private int defaultHops = 10;
	
	static Random random = new Random();

	private static final long serialVersionUID = 1L;

	EventLinkPacket(String sourceServer, String destinationServer, Object payload) {
		this(sourceServer, destinationServer, payload, defaultHops);
	}

	EventLinkPacket(String sourceServer, String[] destinationServers, Object payload) {
		this(sourceServer, destinationServers, payload, defaultHops);
	}
		
	EventLinkPacket(String sourceServer, String destinationServer, Object payload, int timeToLive) {
		this(sourceServer, new String[] {destinationServer}, payload, timeToLive, false, false);
	}
	
	EventLinkPacket(String sourceServer, String[] destinationServer, Object payload, int timeToLive) {
		this(sourceServer, destinationServer, payload, timeToLive, false, false);
	}

	EventLinkPacket(String sourceServer, String destinationServer, Object payload, int timeToLive, boolean requestConfirm, boolean confirmationPacket) {
		this(sourceServer, new String[] {destinationServer}, payload, timeToLive, requestConfirm, confirmationPacket);
	}

	EventLinkPacket(EventLinkPacket eventLinkPacket, String[] destinationServers) {
		this(eventLinkPacket.sourceServer, destinationServers, eventLinkPacket.payload, eventLinkPacket.timeToLive, eventLinkPacket.requestConfirm, eventLinkPacket.confirmationPacket);
	}
	
	EventLinkPacket(String sourceServer, String[] destinationServers, Object payload, int timeToLive, boolean requestConfirm, boolean confirmationPacket) {
		this.sourceServer = sourceServer;
		this.destinationServers = destinationServers;
		this.payload = payload;
		this.timeToLive = timeToLive;
		this.requestConfirm = requestConfirm;
		this.confirmationPacket = confirmationPacket;
		synchronized(random) {
			this.idNum = random.nextLong();
		}

		if(destinationServers[0] == null) {
		try {
			throw new RuntimeException();
		} catch (Exception e) {
			e.printStackTrace();
		}
		}

	}
	
	final public String sourceServer;
	final public String[] destinationServers;
	final public Object payload;
	int timeToLive = 10;
	
	private final boolean requestConfirm;
	private boolean confirmationPacket;
	private final long idNum;
	private long timeStamp = -1;
	
	public String toString() {
		return sourceServer + "->" + Arrays.toString(destinationServers) + " [" + payload + "]";
	}
	
	boolean isConfirmRequired() {
		return requestConfirm;
	}

	boolean isConfirmationPacket() {
		return confirmationPacket;
	}
	
	void setConfirmationPacket(boolean confirmationPacket) {
		this.confirmationPacket = confirmationPacket;
	}
	
	public long getIdNum() {
		return idNum;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	//NOTE: need to reset connection due to object cache
	
}
