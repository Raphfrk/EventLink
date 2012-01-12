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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RoutingTable implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String tableName;

	private final HashMap<String,RoutingTableEntry> tableEntries = new HashMap<String,RoutingTableEntry>();

	private boolean changed = true;

	RoutingTable(String name) {
		this.tableName = name;
	}
	
	@Override
	public synchronized String toString() {
		Iterator<String> itr = tableEntries.keySet().iterator();
		
		StringBuilder sb = new StringBuilder("Table Name: " + tableName + " ");
		
		while(itr.hasNext()) {
			String entryName = itr.next();
			
			RoutingTableEntry tableEntry = tableEntries.get(entryName);
			
			sb.append("[" + entryName + " [" + tableEntry + "]" );
			
		}
		
		return sb.toString();
	}
	
	public synchronized Map<String,RoutingTableEntry> getEntries() {
		
		Set<String> keys = tableEntries.keySet();
		HashMap<String,RoutingTableEntry> tableMap = new HashMap<String,RoutingTableEntry>();
		
		for(String key : keys) {
			RoutingTableEntry current = tableEntries.get(key);
			tableMap.put(key, current.clone());
		}
		return tableMap;
		
	}

	public synchronized boolean deleteEntry(String entryName) {
		if(tableEntries.containsKey(entryName)) {
			tableEntries.remove(entryName);
			changed = true;
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean addEntry(String entryName, String thisServer) {

		RoutingTableEntry tableEntry = new RoutingTableEntry();
		tableEntry.setNextServer("");
		tableEntry.setHops(0);
		tableEntry.setLocation(thisServer);

		if(tableEntries.containsKey(entryName)) {
			RoutingTableEntry current = tableEntries.get(entryName);
			if(current.equal(tableEntry)) {
				return true;
			}
		}

		tableEntries.put(entryName, tableEntry);
		changed = true;
		return true;
	}
	
	public synchronized void clearRoutesThrough(String server) {
		Iterator<String> itr = tableEntries.keySet().iterator();
		
		while(itr.hasNext()) {
			String entryName = itr.next();
			
			RoutingTableEntry tableEntry = tableEntries.get(entryName);
			
			if(tableEntry.getNextServer().equals(server)) {
				itr.remove();
				changed = true;
			}
			
		}
	}

	public synchronized boolean combineTable(String source, String thisServer, RoutingTable other) {

		if(!other.getTableName().equals(tableName)) {
			return false;
		}

		Iterator<String> itr = tableEntries.keySet().iterator();
		while(itr.hasNext()) {
			String entryName = itr.next();
			
			RoutingTableEntry tableEntry = tableEntries.get(entryName);

			RoutingTableEntry otherTableEntry = other.tableEntries.get(entryName);

			if(tableEntry.getNextServer().equals(source)) {
				if(otherTableEntry == null || otherTableEntry.getHops() >= tableEntry.getHops()) {
					changed = true;
					itr.remove();
				}
			}
		}

		for(String entryName : other.tableEntries.keySet()) {
			
			RoutingTableEntry tableEntry = tableEntries.get(entryName);

			RoutingTableEntry otherTableEntry = other.tableEntries.get(entryName);

			if(tableEntry == null || tableEntry.getHops() > otherTableEntry.getHops() + 1 ) {
				if(!otherTableEntry.getNextServer().equals(thisServer)) {
					changed = true;

					RoutingTableEntry newTableEntry = new RoutingTableEntry();
					newTableEntry.setHops(otherTableEntry.getHops() + 1);
					newTableEntry.setLocation(otherTableEntry.getLocation());
					newTableEntry.setNextServer(source);
					tableEntries.put(entryName, newTableEntry);
				}	
			} else if (tableEntry.getNextServer().equals(source) && !tableEntry.getLocation().equals(otherTableEntry.getLocation())) {
				changed = true;
				tableEntry.setLocation(otherTableEntry.getLocation());
			}
		}

		return changed;

	}

	public synchronized String getTableName() {
		return tableName;
	}

	public synchronized boolean getChanged() {
		return changed;
	}

	public synchronized void clearChanged() {
		changed = false;
	}
	
	public synchronized void listToLog(EventLink p) {
		p.log("Routing Table Name: " + getTableName() + ((changed)?(" Changed"):(" Not changed")));
		for(String key : tableEntries.keySet() ) {
			p.log(key + " " + tableEntries.get(key));
		}
	}

}
