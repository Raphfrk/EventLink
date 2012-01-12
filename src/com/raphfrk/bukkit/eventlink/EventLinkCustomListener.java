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

import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

import com.raphfrk.bukkit.eventlinkapi.EventLinkSetupEvent;

public class EventLinkCustomListener extends CustomEventListener {

	final EventLink p;

	EventLinkCustomListener(EventLink p) {
		this.p = p;
	}

	public void onCustomEvent(Event event) {
		if(event instanceof EventLinkMessageEvent) {
			onCustomEvent((EventLinkMessageEvent)event);
		} else if(event instanceof EventLinkSetupEvent) {
			onCustomEvent((EventLinkSetupEvent)event);
		}
	}
	
	public void onCustomEvent(EventLinkMessageEvent eventLinkMessage) {

		String playerName = eventLinkMessage.getTarget();

		Player player;
		if(playerName != null) {
			player = p.getServer().getPlayer(playerName);
		} else {
			return;
		}

		String message = eventLinkMessage.getMessage();
		if(player!=null && message!=null) {

			player.sendMessage(message);

		}
	}
	
}
