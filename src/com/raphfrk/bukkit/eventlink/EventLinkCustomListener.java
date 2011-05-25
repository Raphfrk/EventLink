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