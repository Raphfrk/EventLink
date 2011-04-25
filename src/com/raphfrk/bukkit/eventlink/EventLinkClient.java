package com.raphfrk.bukkit.eventlink;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.Principal;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.bukkit.command.CommandSender;

public class EventLinkClient {


	final EventLink p;

	final CommandSender commandSender;

	final String serverName;
	final Object serverNameSync = new Object();

	final private int portnum;
	final String hostname;

	final private KeyManager[] keyManagers;

	final TrustManager trustManager;

	final String password;
	final File clientFile;

	final Thread t;

	final ClientRunnable clientRunnable;
	
	final File trustFile;
	
	final boolean invite;

	EventLinkClient(EventLink p, CommandSender commandSender, String serverName, File serverFile, File trustFile, String password, String hostname, int portnum, boolean invite) {

		this.p = p;
		
		this.invite = invite;

		this.commandSender = commandSender;

		this.hostname = hostname;

		this.portnum = portnum;

		this.password = password;

		this.clientFile = trustFile;
		
		this.serverName = serverName;

		keyManagers = SSLUtils.getKeyManagers(serverFile, password);

		this.trustFile = trustFile;
		
		if(!invite) {
			trustManager = new ReloadableTrustManager(trustFile, password);
		} else {
			trustManager = new TrustingTrustManager();
		}

		clientRunnable = new ClientRunnable();

		t = new Thread(clientRunnable);

		t.start();

	}

	private class ClientRunnable implements Runnable {

		public void run() {
			
			final KeyManager[] keyManagersLocal = invite?null:keyManagers;

			Socket socket = SSLUtils.getSSLSocket(hostname, portnum, keyManagersLocal, trustManager);

			if(socket==null) {
				MiscUtils.sendAsyncMessage(p, p.server, commandSender, "Unable to connect to " + hostname + ":" + portnum);
				p.log("Unable to connect to " + hostname + ":" + portnum);
				return;
			}

			p.log("Connected to " + hostname + ":" + portnum);

			Principal peer = null;

			Object peerNameObject;			

			SSLSession sslSession = ((SSLSocket)socket).getSession();

			try {
				
				Certificate[] certs = null;

				if(invite) {
					try {
						certs = sslSession.getPeerCertificates();
					} catch (SSLPeerUnverifiedException e) {
						e.printStackTrace();
						p.log("Unable to extract certificate from target server" );
						MiscUtils.sendAsyncMessage(p, p.server, commandSender, "Unable to extract certificate from target server");
						SSLUtils.closeSocket(socket);
						return;
					}

				}

				OutputStream socketOut = socket.getOutputStream();
				InputStream socketIn = socket.getInputStream(); 

				peer = sslSession.getPeerPrincipal();
				
				ObjectOutputStream out = new ObjectOutputStream(socketOut);

				p.log("Sending server name: " + serverName);
				
				out.writeObject(serverName);
				out.flush();

				ObjectInputStream in = new ObjectInputStream(socketIn);
				
				peerNameObject = in.readObject();

				if(!(peerNameObject instanceof String)) {
					p.log("Server name not sent by target server" );
					SSLUtils.closeSocket(socket);
					return;
				}
				String peerName = (String)peerNameObject;
				
				if(!MiscUtils.checkText(peerName)) {
					p.log("Illegal peer name: " + peerName );
					SSLUtils.closeSocket(socket);
					return;
				}

				if(invite) {
					
					String alias = peerName + ";" + hostname + ":" + portnum;
				
					if(certs!=null) {
						if(SSLUtils.addCertificate(trustFile, password, alias, certs[0])) {
							MiscUtils.sendAsyncMessage(p, p.server, commandSender, peerName + " added to trust store");
							p.eventLinkServer.reload();
						}
					}
					p.connectionManager.addConnection(peerName, password, hostname, portnum);
					SSLUtils.closeSocket(socket);
					return;
				}
				
				p.connectionManager.addConnection(peerName, socket, in, out);


			} catch (SSLPeerUnverifiedException pue) {
				//p.log(pue.getLocalizedMessage());
				//pue.printStackTrace();
				p.log("Unable to auth target server: " + hostname + ":" + portnum );
				MiscUtils.sendAsyncMessage(p, p.server, commandSender, "Unable to auth target server");
				SSLUtils.closeSocket(socket);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				SSLUtils.closeSocket(socket);
				return;
			} catch (ClassNotFoundException e) {
				p.log("Unknown class sent by client: " + socket.getInetAddress());

			}

		}

	}

}
