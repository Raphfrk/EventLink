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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.Principal;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class EventLinkServer {

	final EventLink p;

	final String serverName;
	
	final File clientFile;
	final String password;

	final private int portnum;
	final private KeyManager[] keyManagers;

	final ReloadableTrustManager trustManager;

	final ServerRunnable serverRunnable;
	final Thread t;

	EventLinkServer(EventLink p, String serverName, File serverFile, File trustFile, String password, int portnum) {

		this.p = p;

		this.serverName = serverName;
		
		this.portnum = portnum;
		
		this.clientFile = trustFile;
		this.password = password;

		keyManagers = SSLUtils.getKeyManagers(serverFile, password);

		trustManager = new ReloadableTrustManager(trustFile, password);

		serverRunnable = new ServerRunnable();

		t = new Thread(serverRunnable);

		serverRunnable.setThread(t);
		
		t.setName("Event Link Server: port = " + portnum);

		t.start();

	}
	
	void reload() {
		trustManager.reloadTrustStore(clientFile, password);
	}

	boolean stop() {

		if(t == null || serverRunnable == null) {
			return false;
		}

		try {
			serverRunnable.stop();
		} catch (InterruptedException e) {
			return false;
		}

		return true;

	}

	private class ServerRunnable implements Runnable {

		private Object endSync = new Object();
		private boolean end = false;

		private Thread t = null;

		void setThread(Thread t) {
			this.t = t;
		}

		public void stop() throws InterruptedException {

			synchronized(endSync) {
				if(end) {
					return;
				}
				end = true;
			}

			p.log("Stopping server on localhost " + portnum);
			try {
				Socket s = new Socket("localhost" , portnum);
			} catch (UnknownHostException e) {
				return;
			} catch (IOException e) {
				return;
			}

			t.join();
			p.log("Server stopped");

		}

		public void run() {

			ServerSocket server;

			try {
				server = SSLUtils.getSSLServerSocket(p, portnum, keyManagers, trustManager);
			} catch (BindException e) {
				p.log("Unable to bind to " + portnum);
				return;
			}

			try {
				server.setSoTimeout(5000);
			} catch (SocketException e2) {
				p.log("Unable to update socket timeout");
			}

			((SSLServerSocket)server).setNeedClientAuth(false);
			((SSLServerSocket)server).setWantClientAuth(true);

			p.log("Server started on port " + portnum);

			boolean timeout = false;
			while(true) {

				Socket socket = null;

				try {
					socket = server.accept();
				} catch (SocketTimeoutException ste) {
					timeout = true;
				} catch (IOException e1) {
					p.log("Socket exception");
					e1.printStackTrace();
					continue;
				}

				boolean localEnd;

				synchronized(endSync) {
					localEnd = end;
				}

				if(localEnd) {
					try {
						server.close();
					} catch (IOException e) {}
					return;
				}

				if(timeout || socket == null) {
					timeout = false;
					continue;
				}

				p.log("Socket received from " + socket.getInetAddress());

				final Socket finalSocket = socket;
				
				Runnable r = new Runnable() {
					public void run() {
						handleNewConnection(finalSocket);
					}
				};
				
				Thread t2 = new Thread(r);
				t2.start();

			}

		}

	}	

	void handleNewConnection(Socket s) {

		Principal peer = null;

		Object peerNameObject;

		try {
			
			((SSLSocket)s).setWantClientAuth(true);

			SSLSession sslSession = ((SSLSocket)s).getSession();
			
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

			out.writeObject(serverName);
			out.flush();
			
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());

			peerNameObject = in.readObject();
			
			peer = sslSession.getPeerPrincipal();

			if(peerNameObject == null ) {
				p.log("null object received, closing connection");
				SSLUtils.closeSocket(s);
				return;
			} else if(peerNameObject instanceof String) {
				p.log("Connection received from server claiming to be " + (String)peerNameObject);
			} else {
				p.log("First object not a string containing a name (" + peerNameObject.getClass().getName() + "), closing connection");
				SSLUtils.closeSocket(s);
				return;
			}
			
			if( !(peer.toString()).equals("CN=" + peerNameObject) ) {
				p.log( "Cert/Name mismatch in connection: " + peer + " " + peerNameObject);
				SSLUtils.closeSocket(s);
				return;
			}

			p.connectionManager.addConnection((String)peerNameObject, s, in, out);

		} catch (StreamCorruptedException sce) {
			p.log( "Not a valid object stream (corrupted?), closing stream");
			SSLUtils.closeSocket(s);
			return;
		} catch ( SSLPeerUnverifiedException pue ) {
			p.log( "Unable to Auth connection, closing connection");
			SSLUtils.closeSocket(s);
			return;
		} catch (SSLHandshakeException hse) {
			p.log( "Invalid handshake from client, closing connection");
			SSLUtils.closeSocket(s);
			return;
		} catch (SSLException ssle ) {
			p.log("Connection attempt with unencrypted connection from IP: " + s.getInetAddress());
			p.log("Closing stream");
			//ssle.printStackTrace();
			SSLUtils.closeSocket(s);
			return;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			SSLUtils.closeSocket(s);
			return;
		} catch (ClassNotFoundException e) {
			SSLUtils.closeSocket(s);
			return;
		}

	}


}
