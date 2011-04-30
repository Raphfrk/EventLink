package com.raphfrk.bukkit.eventlink;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.event.Event;

public class ConnectionManager {

	private final String serverName;

	private final EventLink p;

	private final Object syncObject = new Object();

	private Thread pollThread;
	private final Object pollSync = new Object();
	private boolean pollEnd = false;
	private String password;

	final HashMap<String,Connection> activeConnections = new HashMap<String,Connection>();

	private boolean alive = true;
	private boolean end = false;
	private final Object endSync = new Object();

	private final Thread t;

	ConnectionManager(EventLink p, String serverName, String password) {

		this.password = password;

		this.serverName = serverName;

		this.p = p;

		t = new Thread(new InObjects());
		t.start();

		pollThread = new Thread(new ConnectionPolling());
		pollThread.start();

	}

	// This is required in order to re-send the mutable object if it has changed
	// Otherwise, the cached object will be sent
	boolean resetConnection(String target) {
		synchronized(activeConnections) {
			if(getEnd()) {
				p.log("Attempting to reset connection while connection manager is stopping");
				return false;
			}
			if(activeConnections.containsKey(target)) {
				Connection targetConnection = activeConnections.get(target);
				targetConnection.reset();
				return true;
			} else {
				return false;
			}

		}
	}
	
	boolean sendObject(String[] targets, Object payload) {
		EventLinkPacket eventLinkPacket = new EventLinkPacket(serverName, targets, payload);

		return sendPacket(eventLinkPacket);
	}

	boolean sendObject(String target, Object payload) {

		EventLinkPacket eventLinkPacket = new EventLinkPacket(serverName, target, payload);

		return sendPacket(eventLinkPacket);

	}

	boolean sendPacket(EventLinkPacket eventLinkPacket) {

		boolean sent = false;

		String[] destinationBackup = eventLinkPacket.destinationServers;
		final int length = destinationBackup.length;

		for(int cnt1=0;cnt1<length;cnt1++) {
			String currentTarget = destinationBackup[cnt1];
			destinationBackup[cnt1] = null;

			if(currentTarget == null) {
				continue;
			}

			if(currentTarget.equals(p.serverName)) {
				String[] temp = new String[1];
				temp[0] = currentTarget;

				processPacket(new EventLinkPacket(eventLinkPacket, temp));
				sent = true;
				continue;
			}

			String currentNextHop = p.routingTableManager.getNextHop("servers", currentTarget);

			ArrayList<String> targets = new ArrayList<String>();
			targets.add(currentTarget);

			if(currentNextHop != null) {

				for(int cnt2=cnt1+1;cnt2<length;cnt2++) {
					String target = destinationBackup[cnt2];
					String nextHop = p.routingTableManager.getNextHop("servers", target);
					if(!target.equals(p.serverName) && nextHop.equals(currentNextHop)) {
						destinationBackup[cnt2] = null;
						targets.add(target);
					}
				}

			}

			int length2 = targets.size();

			String[] temp = new String[length2];

			for(int cnt2=0;cnt2<length2;cnt2++) {
				temp[cnt2] = targets.get(cnt2);
			}
			
			EventLinkPacket newPacket = new EventLinkPacket(eventLinkPacket, temp);

			synchronized(activeConnections) {
				if(getEnd()) {
					p.log("Attempting to send object while connection manager is stopping");
					return false;
				}
				Connection oneHop = activeConnections.get(temp[0]);
				Connection multiHop = activeConnections.get(currentNextHop);
				
				if(activeConnections.containsKey(currentNextHop)) {
					if( (newPacket.timeToLive--) >= 0) {
						sent = true;
						multiHop.send(newPacket);
					}
				} else if(oneHop != null) {
					if( (newPacket.timeToLive--) >= 0) {
						sent = true;
						oneHop.send(newPacket);
					}
				}
			}
		}

		return sent;

	}

	String deleteConnection(String serverName) {
		synchronized(activeConnections) {
			if(getEnd()) {
				return "Connection Manager is shutting down";
			}

			if(activeConnections.containsKey(serverName)) {

				Connection connection = activeConnections.get(serverName);

				if(!connection.getAlive()) {
					return "Connection to " + serverName + " is dead";
				}

				connection.stop();

				return "Sent stop signal to connection";
			} else {
				return "There is no active connection to " + serverName;
			}

		}
	}

	boolean addConnection(String serverName, String password, String hostname, int portnum) {
		return addConnection(serverName, password, hostname, portnum, true);
	}

	boolean addConnection(String serverName, String password, String hostname, int portnum, boolean log) {

		synchronized(activeConnections) {
			if(getEnd()) {
				p.log("Attempting to add a connection while connection manager is stopping");
				return false;
			}
			if(activeConnections.containsKey(serverName) && activeConnections.get(serverName).getAlive()) {
				if(log) {
					p.log(serverName + " already has a connection");
				}
				return false;
			}

			new EventLinkClient(
					p,
					null,
					this.serverName,
					new File(p.pluginDirectory + EventLink.slash + p.serverKeys), 
					new File(p.pluginDirectory + EventLink.slash + p.clientKeys),
					password,
					hostname,
					portnum,
					false
			);

			return true;

		}

	}

	boolean addConnection(String serverName, Socket s, ObjectInputStream in, ObjectOutputStream out ) {

		synchronized(activeConnections) {
			if(getEnd()) {
				p.log("Attempting to start a connection while connection manager is stopping, closing");
				SSLUtils.closeSocket(s);
				return false;
			}
			if(activeConnections.containsKey(serverName) && !activeConnections.get(serverName).getAlive()) {
				activeConnections.remove(serverName);
				p.routingTableManager.clearRoutesThrough(serverName);
			}
			if(activeConnections.containsKey(serverName) && activeConnections.get(serverName).getAlive()) {
				p.log(serverName + " already has a connection, closing");
				SSLUtils.closeSocket(s);
				return false;
			}
			Connection connection = new Connection(this, syncObject, p, s, in, out, serverName);
			activeConnections.put(serverName, connection);
		}

		synchronized(syncObject) {
			syncObject.notify();
		}

		p.log("Connection successfully established with " + serverName );

		p.routingTableManager.sendAllTablesTo(serverName);

		return true;

	}

	boolean isConnected(String serverName) {
		synchronized(activeConnections) {
			if(!activeConnections.containsKey(serverName)) {
				return false;
			}
			return activeConnections.get(serverName).getAlive();
		}
	}

	boolean getEnd() {
		synchronized(endSync) {
			return end;
		}
	}

	void checkTrusted(String password) {
		Enumeration<String> aliases = SSLUtils.getAliases(new File(p.pluginDirectory + p.slash + p.clientKeys), password);
		while(aliases.hasMoreElements()) {
			String current = aliases.nextElement();
			String[] split = (current.split(";"));

			String currentServerName = split[0];

			if(isConnected(currentServerName) || split.length<=1) {
				continue;
			}

			p.log("Attempting to connect to " + currentServerName);

			String hostname = SSLUtils.getHostname(split[1]);
			int portnum = SSLUtils.getPortnum(split[1]);

			new EventLinkClient(
					p,
					null,
					this.serverName,
					new File(p.pluginDirectory + EventLink.slash + p.serverKeys), 
					new File(p.pluginDirectory + EventLink.slash + p.clientKeys),
					password,
					hostname,
					portnum,
					false
			);


		}
	}

	void stop() {

		synchronized(endSync) {
			end = true;
		}

		LinkedList<Connection> connectionsToStop = new LinkedList<Connection>();

		synchronized(activeConnections) {
			for(String current : activeConnections.keySet()) {
				connectionsToStop.add(activeConnections.get(current));
			}
		}

		for(Connection current : connectionsToStop) {
			p.log("Stopping connection to server: " + current.getServerName());
			current.stop();
		}

		for(Connection current : connectionsToStop) {
			p.log("Waiting for connection to: " + current.getServerName());
			while(current.getAlive()) {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {}
			}
		}
		synchronized(activeConnections) {
			if(!activeConnections.isEmpty()) {
				p.log("ERROR: all connections did not end");
				activeConnections.clear();
			}
		}

		p.log("Waiting for connection manager timer to close");

		boolean endTemp = false;
		while(!endTemp) {
			synchronized(pollSync) {
				if(!pollEnd) {
					pollSync.notify();
				}
				endTemp = pollEnd;
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {}
		}

		p.log("Waiting for connection manager to close");

		while(true) {
			synchronized(endSync) {
				endSync.notify();
				if(!alive) {
					p.log("Connection manager closed");
					return;
				}
			}
			synchronized(syncObject) {
				syncObject.notify();
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
		}

	}

	private class ConnectionPolling implements Runnable {

		public void run() {

			while(!getEnd()) {
				checkTrusted(password);
				synchronized(pollSync) {
					try {
						pollSync.wait(60000);
					} catch (InterruptedException e) {
					}
				}
			}

			synchronized(pollSync) {
				pollEnd = true;
			}

		}

	}

	private class InObjects implements Runnable {

		LinkedList<EventLinkPacket> eventLinkPackets = new LinkedList<EventLinkPacket>();

		public void run() {

			while(!getEnd()) {

				synchronized(activeConnections) {
					for(String server : activeConnections.keySet()) {
						EventLinkPacket eventLinkPacket = null;
						Connection connection = activeConnections.get(server);
						do {

							eventLinkPacket = connection.receive();
							if(eventLinkPacket!=null) {
								eventLinkPackets.addLast(eventLinkPacket);
							}
						} while (eventLinkPacket != null );
					}
				}

				EventLinkPacket eventLinkPacket = null;
				if(!eventLinkPackets.isEmpty()) {
					eventLinkPacket = eventLinkPackets.removeFirst();
				}
				while(eventLinkPacket!=null) {

					processPacket(eventLinkPacket);

					if(!eventLinkPackets.isEmpty()) {
						eventLinkPacket = eventLinkPackets.removeFirst();
					} else {
						eventLinkPacket = null;
					}

				}

				synchronized(syncObject) {
					boolean active = false;
					synchronized(activeConnections) {
						for(Connection connection : activeConnections.values()) {
							if(!connection.isEmpty()) {
								active = true;
								break;
							}
						}
					}
					if(!active) {
						try {
							syncObject.wait(10000);
						} catch (InterruptedException e) {}
					}
				}
			}

			synchronized(endSync) {
				alive = false;
			}

		}
	}

	void processPacket(EventLinkPacket eventLinkPacket) {
		
		if(eventLinkPacket == null) {
			return;
		} 
		
		Object payload = eventLinkPacket.payload;

		if(eventLinkPacket.destinationServers == null || eventLinkPacket.destinationServers.length == 0) {
			return;
		} else if(eventLinkPacket.destinationServers.length == 1 && eventLinkPacket.destinationServers[0] == null ) {
			return;
		} else if(eventLinkPacket.destinationServers.length != 1 || (!eventLinkPacket.destinationServers[0].equals(p.serverName))) {
			sendPacket(eventLinkPacket);
		} else if(payload instanceof Ping) {
			processEvent(eventLinkPacket, (Ping)eventLinkPacket.payload);
		} else if(payload instanceof RoutingTable) {
			processEvent(eventLinkPacket, (RoutingTable)eventLinkPacket.payload);
		} else if(payload instanceof Event) {
			processEvent(eventLinkPacket, (Event)eventLinkPacket.payload);
		}
	}

	void processEvent(EventLinkPacket eventLinkPacket, Ping ping) {
		sendObject(eventLinkPacket.sourceServer, new EventLinkMessageEvent(ping.sourcePlayer, "Reply received from " + serverName));
	}

	void processEvent(EventLinkPacket eventLinkPacket, RoutingTable rt) {
		p.routingTableManager.combineTable(eventLinkPacket.sourceServer, rt);
	}

	void processEvent(EventLinkPacket eventLinkPacket, final Event finalEvent) {
		p.getServer().getScheduler().scheduleSyncDelayedTask(p, new Runnable() {

			public void run() {
				p.getServer().getPluginManager().callEvent(finalEvent);
			}

		});
	}

}
