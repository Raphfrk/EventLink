package com.raphfrk.bukkit.eventlink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class MyPropertiesFile {

	static final String slash = System.getProperty("file.separator");

	static HashMap<String,HashMap<String,String>> propertiesDatabase = new HashMap<String,HashMap<String,String>>();

	static Object syncObject = new Object();

	HashMap<String,String> databaseTable;

	String tableName = null;

	String prefix = "";

	public MyPropertiesFile(String filename) {

		tableName = new String(filename);

	}

	public MyPropertiesFile(String prefix, String filename) {

		this.prefix = new String(prefix);

		tableName = new String(filename);

	}
	
	public boolean containsKey(String name) {
		return databaseTable.containsKey(name);
	}

	public void load() {

		String[] lines = MiscUtils.fileToString(prefix + tableName);

		addTable();
		
		if( lines == null ) {
			return;
		}

		for( String line : lines ) {
			
			line = line.trim();
						
			if( line.indexOf('#') == -1 ) {

				String[] split = line.split("=",2);

				if( split.length == 2 ) {
					
					addRecord(split[0],split[1]);

				}
			}

		}

	}

	public void save() {

		ArrayList<String> strings = new ArrayList<String>();

		Iterator<String> itr = databaseTable.keySet().iterator();
		while( itr.hasNext() ) {
			String current = itr.next();
			strings.add(current + "=" + databaseTable.get(current));
		}

		MiscUtils.stringToFile(strings, prefix + tableName);

	}

	public void unLoadTable() {

		synchronized(syncObject) {
			if(!propertiesDatabase.containsKey(tableName)) {
				propertiesDatabase.remove(tableName);
			}
		}

	}

	public void addTable() {

		synchronized(syncObject) {
			if(!propertiesDatabase.containsKey(tableName)) {
				propertiesDatabase.put(tableName, new HashMap<String,String>());
			}
			databaseTable = propertiesDatabase.get(tableName);
		}

	}

	public void addRecord(String name, String value) {
		synchronized(syncObject) {
			databaseTable.put(name, value);		
		}
	}
	
	public int getInt( String name ) {
		synchronized(syncObject) {
			return Integer.parseInt(databaseTable.get(name));	
		}
	}

	public long getLong( String name ) {
		synchronized(syncObject) {
			return Long.parseLong(databaseTable.get(name));		
		}
	}

	public Boolean getBoolean( String name ) {
		synchronized(syncObject) {
			return Boolean.parseBoolean(databaseTable.get(name));	
		}
	}

	public Double getDouble( String name ) {
		synchronized(syncObject) {
			return Double.parseDouble(databaseTable.get(name));	
		}
	}

	public String getString( String name ) {
		synchronized(syncObject) {
			return new String(databaseTable.get(name));		
		}
	}


	public int getInt( String name , int defaultValue ) {
		synchronized(syncObject) {
			if( !databaseTable.containsKey(name)) {
				addRecord(name,Integer.toString(defaultValue));
			}
			return Integer.parseInt(databaseTable.get(name));	
		}
	}

	public long getLong( String name , long defaultValue ) {
		synchronized(syncObject) {
			if( !databaseTable.containsKey(name)) {
				addRecord(name,Long.toString(defaultValue));
			}
			return Long.parseLong(databaseTable.get(name));		
		}
	}

	public Boolean getBoolean( String name , boolean defaultValue ) {
		synchronized(syncObject) {
			if( !databaseTable.containsKey(name)) {
				addRecord(name,Boolean.toString(defaultValue));
			}
			return Boolean.parseBoolean(databaseTable.get(name));	
		}
	}

	public Double getDouble( String name , double defaultValue ) {
		synchronized(syncObject) {
			if( !databaseTable.containsKey(name)) {
				addRecord(name,Double.toString(defaultValue));
			}
			return Double.parseDouble(databaseTable.get(name));	
		}
	}

	public String getString( String name , String defaultValue ) {
		synchronized(syncObject) {
			if( !databaseTable.containsKey(name)) {
				addRecord(name,new String(defaultValue));
			}
			return new String(databaseTable.get(name));		
		}
	}

	public void setInt( String name, int value) {
		addRecord( name, Integer.toString(value));
	}

	public void setLong( String name, long value) {
		addRecord( name, Long.toString(value));
	}
	
	public void setDouble( String name, double value) {
		addRecord( name, Double.toString(value));
	}
	
	public void setBoolean( String name, boolean value) {
		addRecord( name, Boolean.toString(value));
	}
	
	public void setString( String name, String value) {
		addRecord( name, new String(value));
	}
}
