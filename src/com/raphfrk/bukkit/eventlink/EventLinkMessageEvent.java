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

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EventLinkMessageEvent extends Event implements Serializable {

	private static final long serialVersionUID = 2L;
	private static final HandlerList handlers = new HandlerList();
	private final String message;
	private final String target;
	private final String from;

	EventLinkMessageEvent(String target, String message) {
		this.message = message;
		this.target = target;
		this.from = null;
	}

	EventLinkMessageEvent(String from, String target, String message) {
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

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
