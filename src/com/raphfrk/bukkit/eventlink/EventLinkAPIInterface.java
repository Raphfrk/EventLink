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
