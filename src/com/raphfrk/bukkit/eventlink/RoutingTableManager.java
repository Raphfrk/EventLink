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

import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoutingTableManager {
	
	private final Thread t;
	
	private final String password;
	
	private boolean updatePending = false;
	private final Object updateSync = new Object();
	
	EventLink p;
	
	private Object endSync = new Object();
	private boolean end = false;
	
	ConcurrentHashMap<String,RoutingTable> routingTables = new ConcurrentHashMap<String,RoutingTable>();
	
	RoutingTableManager(EventLink p, String password) {
		
		this.p = p;
		
		this.password = password;
		
		RoutingTableUpdater routingTableUpdater = new RoutingTableUpdater();
		
		t = new Thread(routingTableUpdater);
		t.setName("Routing table manager");
		t.start();
		
	}
	
	public void stop() {
		
		synchronized(endSync) {
			end = true;
		}
		
		flagUpdatePending();
		
		try {
			t.join();
		} catch (InterruptedException e) {
		}
		
	}

	public synchronized Map<String,RoutingTableEntry> getEntries(String table) {
		
		RoutingTable routingTable = routingTables.get(table);
		
		if(routingTable == null) {
			return null;
		}
		return routingTable.getEntries();
		
	}
	
	public synchronized String getNextHop(String table, String name) {
		RoutingTable routingTable = routingTables.get(table);
		if(routingTable == null) {
			return null;
		}
		Map<String, RoutingTableEntry> entries = routingTable.getEntries();
		
		if(entries == null) {
			return null;
		}
		
		RoutingTableEntry entry = entries.get(name);
		if(entry == null) {
			return null;
		}
		
		return entry.getNextServer();
	}
	
	public synchronized String getLocation(String table, String name) {
		RoutingTable routingTable = routingTables.get(table);
		if(routingTable == null) {
			return null;
		}
		Map<String, RoutingTableEntry> entries = routingTable.getEntries();
		
		if(entries == null) {
			return null;
		}
		
		RoutingTableEntry entry = entries.get(name);
		if(entry == null) {
			return null;
		}
		
		return entry.getLocation();
	}
	
	public synchronized Set<String> copyKeySet(String table) {
		RoutingTable routingTable = routingTables.get(table);
		if(routingTable == null) {
			return null;
		}
		Map<String, RoutingTableEntry> entries = routingTable.getEntries();
		
		if(entries == null) {
			return null;
		}
		
		LinkedHashSet<String> newSet = new LinkedHashSet<String>();
		
		for(String key : entries.keySet()) {
			newSet.add(key);
		}
		
		return newSet;
	}
	
	public synchronized boolean addEntry(String table, String name) {
		
		if(!routingTables.containsKey(table)) {
			RoutingTable routingTable = new RoutingTable(table);
			routingTables.put(table, routingTable);
		}
		
		RoutingTable routingTable = routingTables.get(table);
		
		routingTable.addEntry(name, p.serverName);
		
		flagUpdatePending();
		
		return true;
	}
	
	public synchronized boolean deleteTable(String table) {
		if(!routingTables.containsKey(table)) {
			return false;
		}
		
		routingTables.remove(table);
		flagUpdatePending();
		return true;
	}
	
	public synchronized boolean deleteEntry(String table, String name) {
		
		if(!routingTables.containsKey(table)) {
			return false;
		}
		
		RoutingTable routingTable = routingTables.get(table);
		
		if(!routingTable.deleteEntry(name)) {
			return false;
		}
		
		flagUpdatePending();
		
		return true;
	}
	
	public synchronized boolean combineTable(String source, RoutingTable routingTable) {
		
		if(routingTable == null) {
			return false;
		}
		String table = routingTable.getTableName();
		
		if(!routingTables.containsKey(table)) {
			RoutingTable rt = new RoutingTable(table);
			routingTables.put(table, rt);
		}
		
		RoutingTable rt = routingTables.get(table);
		
		boolean ret = rt.combineTable(source, p.serverName, routingTable);
		
		sendUpdatedTables();
		flagUpdatePending();
		
		return ret;
		
	}
	
	public synchronized void listTablesToLog() {
		
		for(String key:routingTables.keySet()) {
			RoutingTable table = routingTables.get(key);
			table.listToLog(p);
		}

	}
	
	private synchronized void sendUpdatedTables() {
		
		for(String key:routingTables.keySet()) {
			
			RoutingTable table = routingTables.get(key);
			if(table.getChanged()) {
				table.clearChanged();
				sendTableToAll(table);
			}
			
		}
		
	}
	
	public synchronized void clearRoutesThrough(String server) {

		for(String key:routingTables.keySet()) {
			
			RoutingTable table = routingTables.get(key);
			table.clearRoutesThrough(server);
			
		}
		flagUpdatePending();
	}
	
	private synchronized void sendAllTablesToAll() {
		Enumeration<String> aliases = SSLUtils.getAliases(new File(p.pluginDirectory + EventLink.slash + p.clientKeys), password);
		
		while(aliases.hasMoreElements()) {
			
			String current = aliases.nextElement();
			String currentServerName = (current.split(";"))[0];
			
			if(current.contains(";") && p.connectionManager != null && p.connectionManager.isConnected(currentServerName)) {
				sendAllTablesTo(currentServerName);
			}	
		}
	}
	
	private synchronized void sendTableToAll(RoutingTable table) {
		
		Enumeration<String> aliases = SSLUtils.getAliases(new File(p.pluginDirectory + EventLink.slash + p.clientKeys), password);
		
		while(aliases.hasMoreElements()) {
			
			String current = aliases.nextElement();
			String currentServerName = (current.split(";"))[0];
			
			if(current.contains(";") && p.connectionManager != null && p.connectionManager.isConnected(currentServerName)) {
				sendTable(currentServerName, table);
			}	
		}
	}
	
	private synchronized void sendTable(String target, RoutingTable table) {
		
		p.connectionManager.sendObject(target, table);
		
	}
	
	public synchronized void sendAllTablesTo(String target) {
		for(String key : routingTables.keySet()) {
			sendTable(target, routingTables.get(key));
		}
	}
	
	private void flagUpdatePending() {
		synchronized(updateSync) {
			updatePending = true;
			updateSync.notify();
		}
	}
	
	private class RoutingTableUpdater implements Runnable {
		
		public void run() {
		
			long lastUpdate = -1;
			
			int cnt = 0;
			
			boolean localEnd = false;
			
			while(!localEnd) {
				cnt++;
				
				if(cnt>=20) {
					cnt = 0;
					long currentTime = System.currentTimeMillis();

					if(currentTime > lastUpdate + 60000) {
						lastUpdate = currentTime;
						sendAllTablesToAll();
					}
				}
				
				boolean resend = false;
				synchronized(updateSync) {
					if(updatePending) {
						resend = true;
						updatePending = false;
					}
				}
				if(resend) {
					sendUpdatedTables();
				} 
				synchronized(updateSync) {
					try {
						updateSync.wait(1000);
					} catch (InterruptedException e) {
					}
				}

				synchronized(endSync) {
					localEnd = end;
				}
			}
			
		}
		
		
	}
	

}
