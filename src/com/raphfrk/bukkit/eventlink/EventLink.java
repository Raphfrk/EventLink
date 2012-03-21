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
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.raphfrk.bukkit.eventlinkapi.EventLinkAPI;


public class EventLink extends JavaPlugin {

	String serverName = "";
	int portnum = 25365;
	Integer defaultTimeToLive = 10;
	String clientKeys = "keysclient";
	String serverKeys = "keysserver";
	private String password = "key_password";
	String algorithm = "RSA";
	String certAlgorithm = "DSA";
	int keySize;
	HashSet<String> admins = new HashSet<String>();
	boolean deadlockEnable;

	File pluginDirectory;

	static MiscUtils.LogInstance logger = MiscUtils.getLogger("[EventLink]");

	EventLinkServer eventLinkServer = null;

	boolean nameUpdated = false;

	static final String slash = System.getProperty("file.separator");

	Server server;
	
	KillableThread deadlock;

	ConnectionManager connectionManager;
	RoutingTableManager routingTableManager;

	PluginManager pm;
	
	EventLinkAPIInterface eventLinkAPIInterface = new EventLinkAPIInterface(this);

	EventLinkCustomListener customListener = new EventLinkCustomListener(this);
	EventLinkPlayerListener playerListener = new EventLinkPlayerListener(this);
	EventLinkWorldListener worldListener = new EventLinkWorldListener(this);

	public void onLoad() {
		ServicesManager sm = super.getServer().getServicesManager();
		
		sm.register(EventLinkAPI.class, eventLinkAPIInterface, this, ServicePriority.Normal);
	}
	
	public void onEnable() {

		String name = "EventLink";

		pm = getServer().getPluginManager();
		server = getServer();

		logger.setLogPrefix(getServer().getLogger(), "[EventLink]");

		log(name + " initialized");

		pluginDirectory = this.getDataFolder();

		if( !readConfig() ) {
			log("Unable to read configs or create dirs, Eventlink failed to start");
			return;
		}

		if(!this.serverName.trim().equals("")) {
			nameUpdated = true;

			createCertFiles();

			eventLinkServer = new EventLinkServer(
					this, 
					this.serverName,
					new File(pluginDirectory + slash + serverKeys), 
					new File(pluginDirectory + slash + clientKeys),
					this.password,
					this.portnum
			);

			connectionManager = new ConnectionManager(this, serverName, password);
			routingTableManager = new RoutingTableManager(this, password);
			addServer();
			addOnlinePlayers();
			addWorlds();

		} else {
			log("Unable to start server, server name not set");
			pm.disablePlugin(this);
			return;
		}

		pm.registerEvents(customListener, this);
		pm.registerEvents(playerListener, this);
		pm.registerEvents(worldListener, this);
		
		System.out.println("Registered events");

		if(!this.serverName.trim().equals("")) {
			addServer();
			addOnlinePlayers();
			addWorlds();
		}
		
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				checkWorlds();
			}
		}, 20L);
		
		if (deadlockEnable) {
			deadlock = new DeadlockMonitor();
			deadlock.setName("Deadlock poll thread");
			deadlock.start();
		}
		
	}
	
	public void checkWorlds() {
		
		for(World world : getServer().getWorlds()) {
			routingTableManager.addEntry("worlds", world.getName());
		}

		
	}
	
	public void onDisable() {

		delServer();
		delOnlinePlayers();
		delWorlds();

		if(eventLinkServer!=null) {
			eventLinkServer.stop();
			eventLinkServer = null;
		}
		if(connectionManager!=null) {
			connectionManager.stop();
			connectionManager = null;
		}
		if(routingTableManager!=null) {
			routingTableManager.stop();
			routingTableManager = null;
		}
		
		if (deadlock != null) {
			deadlock.kill();
		}

	}

	private void addOnlinePlayers() {
		Player[] players = getServer().getOnlinePlayers();
		for(Player player : players) {
			routingTableManager.addEntry("players", player.getName());
		}
	}

	private void addWorlds() {
		List<World> worlds = getServer().getWorlds();
		for(World world : worlds) {
			routingTableManager.addEntry("worlds", world.getName());
		}
	}

	private void delOnlinePlayers() {
		Player[] players = getServer().getOnlinePlayers();
		for(Player player : players) {
			routingTableManager.deleteEntry("players", player.getName());
		}
		routingTableManager.deleteTable("players");
	}

	private void delWorlds() {
		List<World> worlds = getServer().getWorlds();
		for(World world : worlds) {
			routingTableManager.deleteEntry("worlds", world.getName());
		}
		routingTableManager.deleteTable("worlds");
	}

	private void addServer() {
		if(!this.serverName.trim().equals("")) {
			routingTableManager.addEntry("servers", this.serverName);
		}
	}

	private void delServer() {
		if(!this.serverName.trim().equals("")) {
			routingTableManager.deleteEntry("servers", this.serverName);
		}
		routingTableManager.deleteTable("servers");
	}

	private boolean readConfig() {

		if( !pluginDirectory.exists() ) {
			pluginDirectory.mkdirs();
		}

		MyPropertiesFile pf;
		try {
			pf = new MyPropertiesFile(new File( pluginDirectory , "eventlink.txt").getCanonicalPath());
		} catch (IOException e) {
			return false;
		}

		pf.load();

		this.serverName = pf.getString("server_name" , "");
		this.portnum = pf.getInt("portnum" , 25365);
		this.defaultTimeToLive = pf.getInt("initial_time_to_live" , 10);
		this.clientKeys = pf.getString("client_keys" , "keysclient");
		this.serverKeys = pf.getString("server_keys" , "keysserver");
		this.password = pf.getString("password" , "key_password");
		this.algorithm = pf.getString("algorithm" , "RSA");
		this.certAlgorithm = pf.getString("cert_algorithm" , "SHA512WITHRSA");
		this.keySize = pf.getInt("key_size" , 512);
		this.deadlockEnable = pf.getBoolean("deadlock_monitor", false);

		String adminString = pf.getString("admin_list", "");
		for( String current : adminString.split(",")) {
			admins.add(current.toLowerCase());
		}

		pf.save();

		return true;

	}

	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {

		if(args.length == 0) {
			return false;
		}

		if(!commandSender.isOp()) {
			commandSender.sendMessage("You do not have sufficient user level for this command");
			return true;
		}

		if(command.getName().equals("eventlink")) {

			if(commandSender instanceof Player && args[0].equalsIgnoreCase("itemgen") && args.length > 2) {
				Player player = (Player)commandSender;
				try {
					int typeId = Integer.parseInt(args[1]);
					int amount = Integer.parseInt(args[2]);
					player.getInventory().addItem(new ItemStack(typeId, amount));
				} catch (NumberFormatException nfe) {
					player.sendMessage("Unable to parse " + args[1] + " and " + args[2] + " as integers");
				}
			}
					
					if(args[0].equalsIgnoreCase("add")) {
				if( args.length > 1 ) {
					new EventLinkClient(
							this,
							commandSender,
							serverName,
							new File(pluginDirectory + slash + serverKeys), 
							new File(pluginDirectory + slash + clientKeys),
							password,
							SSLUtils.getHostname(args[1]),
							SSLUtils.getPortnum(args[1]),
							true
					);
					return true;
				}
			} else if(args[0].equals("list")) {
				Enumeration<String> aliases = SSLUtils.getAliases(new File(pluginDirectory + slash + clientKeys), password);
				if(!aliases.hasMoreElements()) {
					commandSender.sendMessage("No trusted servers added");
					return true;
				} else {
					commandSender.sendMessage("Trusted servers: (*=connected)");
					while(aliases.hasMoreElements()) {
						String current = aliases.nextElement();
						String currentServerName = (current.split(";"))[0];
						if(current.contains(";")) {
							current = "<" + current.replaceFirst(";", "> ");
							if(connectionManager != null && connectionManager.isConnected(currentServerName)) {
								current += " *";
							}
						}
						commandSender.sendMessage(current);
					}
					return true;
				}
			} else if(args.length>1 && (args[0].equals("delete") || args[0].equals("del"))) {
				commandSender.sendMessage(SSLUtils.removeCertificate(new File(pluginDirectory + slash + clientKeys), this.password, args[1]));
				commandSender.sendMessage(connectionManager.deleteConnection(args[1]));
				eventLinkServer.reload();
				return true;
			} else if(args[0].equals("routes")) {
				routingTableManager.listTablesToLog();
				return true;
			} else if(args[0].equals("refresh")) {
				if(connectionManager != null) {
					commandSender.sendMessage("Refreshing connections");
					connectionManager.checkTrusted(password);
				}
				return true;
			} else if(args[0].equals("ping") && args.length > 1 && commandSender instanceof Player) {
				String[] targets = new String[args.length-1];
				for(int cnt=0;cnt<args.length-1;cnt++) {
					targets[cnt] = args[cnt+1];
				}
				if(!connectionManager.sendObject(targets, new Ping(((Player)commandSender).getName()))) {
					if(args.length==2) {
						commandSender.sendMessage(args[1] + " is not a valid ping target");
					} else {
						commandSender.sendMessage("unknown target list");
					}
				}
				return true;
			} else if(args[0].equals("who")) {
				Map<String,RoutingTableEntry> playerMap = routingTableManager.getEntries("players");
				commandSender.sendMessage("Players online:");
				if(playerMap != null) {
					for(String key : playerMap.keySet()) {
						commandSender.sendMessage(key + " (" + playerMap.get(key).getLocation() + ")");
					}
				}
				return true;
			}

		}

		return false;

	}

	private void createCertFiles() {
		File serverFile = new File(pluginDirectory + slash + serverKeys);
		if(!serverFile.exists()) {
			log("No server key/cert file deteted, attempting creation");

			SSLUtils.generateCertificateFile(
					serverFile, 
					this.keySize, 
					this.password, 
					this.algorithm, 
					this.certAlgorithm, 
					"CN=" + serverName, 
					false);

			if(!serverFile.exists()) {
				log("Creation failed");
				return;
			}

		}


		File clientFile = new File(pluginDirectory + slash + clientKeys);
		if(!clientFile.exists()) {

			//Certificate cert = SSLUtils.getCertificate(serverFile, this.password);
			//PrivateKey privateKey = SSLUtils.getPrivateKey(serverFile, this.password);

			log("No client trust/cert file deteted, attempting creation");
			SSLUtils.createKeyFile(
					clientFile, 
					this.password, 
					null, //new KeyPair(cert.getPublicKey(), privateKey), 
					null, //cert, 
					false);

			if(!clientFile.exists()) {
				log("Creation failed");
				return;
			}
		}

	}

	public void log(String message) {
		logger.log(message);
	}

}

