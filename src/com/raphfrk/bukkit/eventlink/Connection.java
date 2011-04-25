package com.raphfrk.bukkit.eventlink;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

public class Connection  {

	private final EventLink p;

	private final Socket s;

	private final ConnectionManager connectionManager;

	private final String serverName;

	private final ObjectInputStream in;
	private final ObjectOutputStream out;

	private boolean end = false;
	private final Object endSync = new Object();

	private final InConnection inConnection;
	private final OutConnection outConnection;

	private final Thread inThread;
	private final Thread outThread;

	private final Object syncObject;

	private boolean endIn = false;
	private boolean endOut = false;
	
	Connection(ConnectionManager connectionManager, Object syncObject, EventLink p, Socket s, ObjectInputStream in, ObjectOutputStream out, String serverName) {
		this.connectionManager = connectionManager;
		this.p = p;
		this.s = s;
		this.serverName = serverName;
		this.syncObject = syncObject;

		try {
			s.setSoTimeout(1000);
		} catch (SocketException e) {
			p.log("Failed to set socket timeout");
		}

		this.out = out;
		this.in = in;
		if(out==null||in==null) {
			end = true;
		}

		outConnection = new OutConnection();
		outThread = new Thread(outConnection);
		outThread.start();

		inConnection = new InConnection();
		inThread = new Thread(inConnection);
		inThread.start();

	}
	
	String getServerName() {
		return serverName;
	}
	
	void stop() {
		synchronized(endSync) {
			end = true;
		}
		outConnection.sync();
	}

	boolean getEnd() {
		synchronized(endSync) {
			return end;
		}
	}

	boolean getAlive() {
		synchronized(endSync) {
			return (!endIn) || (!endOut); 
		}
	}

	public void send(EventLinkPacket eventLinkPacket) {
		outConnection.send(eventLinkPacket);
	}

	public void reset() {
		outConnection.reset();
	}

	private class OutConnection implements Runnable {

		private LinkedList<EventLinkPacket> sendQueue = new LinkedList<EventLinkPacket>();

		public void send(EventLinkPacket eventLinkPacket) {

			synchronized(sendQueue) {
				sendQueue.addLast(eventLinkPacket);
				sendQueue.notify();
			}

		}

		final Object resetSync = new Object();
		boolean reset = false;


		public void reset() {
			synchronized(resetSync) {
				reset = true;
			}
			synchronized(sendQueue) {
				sendQueue.notify();
			}

		}
		
		public void sync() {

			synchronized(sendQueue) {
				sendQueue.notify();
			}

		}

		public void run() {

			while(!getEnd()) {

				Object next = null;

				while(next == null && !getEnd()) {
					synchronized(sendQueue) {
						if(sendQueue.isEmpty()) {
							try {
								sendQueue.wait();
							} catch (InterruptedException e) {
							}
						} else {
							next = sendQueue.removeFirst();
						}
					}
				}
				if(getEnd()) {
					continue;
				}
				synchronized(resetSync) {
					if(reset) {
						reset = false;
						try {
							out.reset();
						} catch (IOException e) {
							p.log("Object reset error with " + serverName);
							stop();
							continue;
						}
					}
				}
				try {
					out.writeObject(next);
				} catch (IOException e) {
					p.log("Object write error to " + serverName);
					stop();
					continue;
				}
			}
			synchronized(connectionManager.activeConnections) {
				synchronized(endSync) {
					endOut = true;
					if(endIn) {
						p.log("Closing connection to " + serverName);
						connectionManager.activeConnections.remove(serverName);
						p.routingTableManager.clearRoutesThrough(serverName);
					}
				}
			}
			synchronized(endSync) {
				endOut = true;
			}
			synchronized(syncObject) {
				syncObject.notify();
			}
		}
	}

	public EventLinkPacket receive() {
		return inConnection.receive();
	}

	public boolean isEmpty() {
		return inConnection.isEmpty();
	}


	private class InConnection implements Runnable {

		private LinkedList<EventLinkPacket> receiveQueue = new LinkedList<EventLinkPacket>();

		public EventLinkPacket receive() {

			synchronized(receiveQueue) {
				if(receiveQueue.isEmpty()) {
					return null;
				} else {
					return receiveQueue.removeFirst();
				}
			}

		}

		public boolean isEmpty() {

			synchronized(receiveQueue) {
				return receiveQueue.isEmpty();
			}

		}


		public void run() {

			while(!getEnd()) {

				Object obj = null;
				try {
					obj = in.readObject();
				} catch (SocketTimeoutException ste) {
					continue;
				} catch (SocketException se) {
					stop();
					continue;
				} catch (EOFException eof) {
					stop();
					continue;
				} catch (IOException e) {
					p.log("IO Error with connection from: " + serverName);
					e.printStackTrace();
					stop();
					continue;
				} catch (ClassNotFoundException e) {
					p.log("Received unknown class from: " + serverName);
					stop();
					continue;
				} 

				if(!(obj instanceof EventLinkPacket)) {
					p.log("Non-packet received (" + obj.getClass() + "): " + serverName);
					stop();
					continue;
				}

				EventLinkPacket eventLinkPacket = (EventLinkPacket)obj;

				synchronized(receiveQueue) {
					if(eventLinkPacket!=null) {
						receiveQueue.addLast(eventLinkPacket);
						synchronized(syncObject) {
							syncObject.notify();
						}
					}

				}
			}
			SSLUtils.closeSocket(s);

			synchronized(connectionManager.activeConnections) {
				synchronized(endSync) {
					endIn = true;
					if(endOut) {
						p.log("Closing connection to " + serverName);
						connectionManager.activeConnections.remove(serverName);
						p.routingTableManager.clearRoutesThrough(serverName);
					}
				}
			}
			synchronized(syncObject) {
				syncObject.notify();
			}
		}
	}
}
