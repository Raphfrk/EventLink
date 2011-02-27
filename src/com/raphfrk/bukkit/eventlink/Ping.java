package com.raphfrk.bukkit.eventlink;

import java.io.Serializable;

public class Ping implements Serializable {

	private static final long serialVersionUID = 1L;
	final String sourcePlayer;
	
	Ping(String player) {
		this.sourcePlayer = player;
	}
	
}
