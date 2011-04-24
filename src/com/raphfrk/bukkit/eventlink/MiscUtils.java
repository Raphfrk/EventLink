package com.raphfrk.bukkit.eventlink;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public class MiscUtils {

	static final private Object logSync = new Object();
	static private Logger log = Logger.getLogger("Minecraft");
	static private String logPrefix = "";

	static void setLogger(Logger log, String prefix) {
		synchronized(logSync) {
			MiscUtils.log = log;
			MiscUtils.logPrefix = prefix;
		}
	}

	static void log( String message ) {
		synchronized(logSync) {
			log.info( logPrefix + " " + message);
		}
	}

	static void sendAsyncMessage(Plugin plugin, Server server, CommandSender commandSender, String message) {
		sendAsyncMessage(plugin, server, commandSender, message, 0);
	}

	static void sendAsyncMessage(Plugin plugin, Server server, CommandSender commandSender, String message, long delay) {
		if( commandSender == null ) return;

		final CommandSender finalCommandSender = commandSender;
		final String finalMessage = message;

		server.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				finalCommandSender.sendMessage(finalMessage);
			}
		}, delay);
	}

	static void stringToFile( ArrayList<String> string , String filename ) {

		File portalFile = new File( filename );

		BufferedWriter bw;

		try {
			bw = new BufferedWriter(new FileWriter(portalFile));
		} catch (FileNotFoundException fnfe ) {
			log.info("[Serverport] Unable to write to file: " + filename );
			return;
		} catch (IOException ioe) {
			log.info("Serverport] Unable to write to file: " + filename );
			return;
		}

		try {
			for( Object line : string.toArray() ) {
				bw.write((String)line);
				bw.newLine();
			}
			bw.close();
		} catch (IOException ioe) {
			log.info("[Serverport] Unable to write to file: " + filename );
			return;
		}

	}

	static String[] fileToString( String filename ) {

		File portalFile = new File( filename );

		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(portalFile));
		} catch (FileNotFoundException fnfe ) {
			log.info("[Serverport] Unable to open file: " + filename );
			return null;
		} 

		StringBuffer sb = new StringBuffer();

		String line;

		try {
			while( (line=br.readLine()) != null ) {
				sb.append( line );
				sb.append( "\n" );

			}
			br.close();
		} catch (IOException ioe) {
			log.info("[Serverport] Error reading file: " + filename );
			return null;
		}

		return( sb.toString().split("\n") );
	}

	static boolean checkText( String text ) {

		if( text.length() > 15 ) {
			return false;
		}

		return text.matches("^[a-zA-Z0-9\\.\\-]+$");
	}


}
