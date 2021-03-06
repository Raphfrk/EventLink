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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.event.Event;

public class ConnectionManager {

	private final String serverName;

	private final EventLink p;

	private final Object syncObject = new Object();

	private KillableThread pollThread;
	private final Object pollSync = new Object();
	private String password;

	final ConcurrentHashMap<String,Connection> activeConnections = new ConcurrentHashMap<String,Connection>();

	private AtomicBoolean end = new AtomicBoolean(false);
	@SuppressWarnings("unused")
	private final Object endSync = new Object();

	private final KillableThread t;

	ConnectionManager(EventLink p, String serverName, String password) {

		this.password = password;

		this.serverName = serverName;

		this.p = p;

		t = new InObjects();
		t.start();

		pollThread = new ConnectionPolling();
		pollThread.setName("Poll Thread");
		pollThread.start();

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

			Connection targetConnection = null;
			
			synchronized(activeConnections) {
				if(getEnd()) {
					p.log("Attempting to send object while connection manager is stopping");
					return false;
				}
				Connection oneHop = temp[0]==null?null:activeConnections.get(temp[0]);
				Connection multiHop = currentNextHop==null?null:activeConnections.get(currentNextHop);
				
				if(currentNextHop != null && activeConnections.containsKey(currentNextHop)) {
					if( (newPacket.timeToLive--) >= 0) {
						sent = true;
						targetConnection = multiHop;
					}
				} else if(oneHop != null) {
					if( (newPacket.timeToLive--) >= 0) {
						sent = true;
						targetConnection = oneHop;
					}
				}
			}
			if (targetConnection != null) {
				targetConnection.send(newPacket);
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

				connection.interruptConnection();

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

		boolean clearRoutes = false;
		synchronized(activeConnections) {
			if(getEnd()) {
				p.log("Attempting to start a connection while connection manager is stopping, closing");
				SSLUtils.closeSocket(s);
				return false;
			}
			if(activeConnections.containsKey(serverName) && !activeConnections.get(serverName).getAlive()) {
				activeConnections.remove(serverName);
				clearRoutes = true;
			}
			if(activeConnections.containsKey(serverName) && activeConnections.get(serverName).getAlive()) {
				p.log(serverName + " already has a connection, closing old connection");
				deleteConnection(serverName);
			}
			Connection connection = new Connection(this, syncObject, p, s, in, out, serverName);
			activeConnections.put(serverName, connection);
		}
		
		if (clearRoutes) {
			p.routingTableManager.clearRoutesThrough(serverName);
		}

		synchronized(syncObject) {
			syncObject.notify();
		}

		p.log("Connection successfully established with " + serverName );

		p.routingTableManager.sendAllTablesTo(serverName);

		return true;

	}

	boolean isConnected(String serverName) {
		Connection connection;
		synchronized(activeConnections) {
			if(!activeConnections.containsKey(serverName)) {
				return false;
			}
			connection = activeConnections.get(serverName);
		}
		return connection.getAlive();
	}

	boolean getEnd() {
		return end.get();
	}

	void checkTrusted(String password) {
		Enumeration<String> aliases = SSLUtils.getAliases(new File(p.pluginDirectory + EventLink.slash + p.clientKeys), password);
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

		end.set(true);

		LinkedList<Connection> connectionsToStop = new LinkedList<Connection>();

		synchronized(activeConnections) {
			for(String current : activeConnections.keySet()) {
				connectionsToStop.add(activeConnections.get(current));
			}
		}

		for(Connection current : connectionsToStop) {
			p.log("Stopping connection to server: " + current.getServerName());
			current.interruptConnection();
		}

		for(Connection current : connectionsToStop) {
			p.log("Waiting for connection to: " + current.getServerName());
			try {
				while(current.joinConnection()) {
					p.log("Living threads: " + current.whichAlive());
					current.interruptConnection();
				}
			} catch (InterruptedException e1) {
			}
		}
		
		synchronized(activeConnections) {
			if(!activeConnections.isEmpty()) {
				p.log("ERROR: all connections did not end");
				activeConnections.clear();
			}
		}

		p.log("Waiting for connection manager timer to close");

		try {
			pollThread.interrupt();
			pollThread.join();
		} catch (InterruptedException e) {
		}

	}

	private class ConnectionPolling extends KillableThread {

		public void run() {

			while(!this.killed()) {
				checkTrusted(password);
				synchronized(pollSync) {
					try {
						pollSync.wait(60000);
					} catch (InterruptedException e) {
						kill();
					}
				}
			}

		}

	}

	private class InObjects extends KillableThread {

		LinkedList<EventLinkPacket> eventLinkPackets = new LinkedList<EventLinkPacket>();

		public void run() {

			while(!killed()) {

				Collection<Connection> connections;
				synchronized(activeConnections) {
					connections = activeConnections.values();
				}
				
				for(Connection c : connections) {
					EventLinkPacket eventLinkPacket = null;

					do {
						eventLinkPacket = c.receive();
						if(eventLinkPacket!=null) {
							eventLinkPackets.addLast(eventLinkPacket);
						}
					} while (eventLinkPacket != null );
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
						} catch (InterruptedException e) {
							kill();
						}
					}
				}
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
