package com.raphfrk.bukkit.eventlink;

import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

public class EventLinkCustomListener extends CustomEventListener {

	final EventLink p;

	EventLinkCustomListener(EventLink p) {
		this.p = p;
	}

	public void onCustomEvent(Event event) {
		if(event instanceof EventLinkMessageEvent) {
			onCustomEvent((EventLinkMessageEvent)event);
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