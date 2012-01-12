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
import org.bukkit.plugin.Plugin;


public class MiscUtils {
	
	public static LogInstance getLogger(String prefix) {
		return new LogInstance(prefix);
	}
	
	public static LogInstance defaultLog = new LogInstance();
	
	public static class LogInstance {
		
		public LogInstance() {
			this.log = Logger.getLogger("Minecraft");
			this.logPrefix = "[EventLink-Default Log]";
		}
		
		public LogInstance(String prefix) {
			this.log = Logger.getLogger("Minecraft");
			this.logPrefix = prefix;
		}
		
		public LogInstance(Logger log, String prefix) {
			this.log = log;
			this.logPrefix = prefix;
		}
		
		final private Object logSync = new Object();
		
		private Logger log = Logger.getLogger("Minecraft");
		private String logPrefix = "";
		
		public void setLogPrefix(Logger log, String prefix) {
			synchronized(logSync) {
				this.log = log;
				this.logPrefix = prefix;
			}
		}
		
		public void log( String message ) {
			synchronized(logSync) {
				log.info( logPrefix + " " + message);
			}
		}
		
	}

	public static void sendAsyncMessage(Plugin plugin, Server server, CommandSender commandSender, String message) {
		sendAsyncMessage(plugin, server, commandSender, message, 0);
	}

	public static void sendAsyncMessage(Plugin plugin, Server server, CommandSender commandSender, String message, long delay) {
		if( commandSender == null ) return;

		final CommandSender finalCommandSender = commandSender;
		final String finalMessage = message;

		server.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				finalCommandSender.sendMessage(finalMessage);
			}
		}, delay);
	}

	public static void stringToFile( ArrayList<String> string , String filename ) {

		File portalFile = new File( filename );

		BufferedWriter bw;

		try {
			bw = new BufferedWriter(new FileWriter(portalFile));
		} catch (FileNotFoundException fnfe ) {
			defaultLog.log("Unable to write to file: " + filename );
			return;
		} catch (IOException ioe) {
			defaultLog.log("Unable to write to file: " + filename );
			return;
		}

		try {
			for( Object line : string.toArray() ) {
				bw.write((String)line);
				bw.newLine();
			}
			bw.close();
		} catch (IOException ioe) {
			defaultLog.log("Unable to write to file: " + filename );
			return;
		}

	}

	public static String[] fileToString( String filename ) {

		File portalFile = new File( filename );

		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(portalFile));
		} catch (FileNotFoundException fnfe ) {
			defaultLog.log("[Serverport] Unable to open file: " + filename );
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
			defaultLog.log("Error reading file: " + filename );
			return null;
		}

		return( sb.toString().split("\n") );
	}

	public static boolean checkText( String text ) {

		if( text.length() > 15 ) {
			return false;
		}

		return text.matches("^[a-zA-Z0-9\\.\\-]+$");
	}


}
