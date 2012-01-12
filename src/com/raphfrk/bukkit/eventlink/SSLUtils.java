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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class SSLUtils {

	// Don't forget to sync random
	static final SecureRandom random = new SecureRandom();
	static final Object randomLock = new Object();

	static public final Object fileLock = new Object();

	static boolean generateCertificateFile(File file, int keySize, String password, String algorithm, String certificateAlgorithm, String certificateName, boolean forceWrite) {

		KeyPair keyPair;
		X509Certificate cert;
		X509V3CertificateGenerator certGen = null;

		String providerName = "BC";

		if (Security.getProvider(providerName) == null) {
			Security.addProvider(new BouncyCastleProvider());
			if (Security.getProvider(providerName) == null) {
				EventLink.logger.log( "Crypt libray (" + providerName + ") provider not installed");
				return false;
			}
		}

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);

			synchronized(randomLock) {
				keyPairGenerator.initialize(keySize, random);
			}

			keyPair = KeyPairGenerator.getInstance(algorithm).generateKeyPair();

			certGen = new X509V3CertificateGenerator();

			certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
			certGen.setIssuerDN(new X500Principal(certificateName));
			certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
			certGen.setNotAfter(new Date(System.currentTimeMillis() + 365*24*60*60*1000));
			certGen.setSubjectDN(new X500Principal(certificateName));
			certGen.setPublicKey(keyPair.getPublic());
			certGen.setSignatureAlgorithm(certificateAlgorithm);

			cert = certGen.generate(keyPair.getPrivate(), "BC");
		} catch ( IllegalArgumentException iae ) {
			EventLink.logger.log("Unable to find provider (BC)");
			iae.printStackTrace();
			if( certGen != null ) {
				Iterator itr = certGen.getSignatureAlgNames();
				while( itr.hasNext() ) {
					System.out.println( "Algorithm: " + itr.next() );
				}
			}
			return false;
		} catch( NoSuchProviderException nspe ) {
			EventLink.logger.log("Unable to find provider (BC)");
			nspe.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException nsa ) {
			EventLink.logger.log("Unable to implement algorithm (" + certificateAlgorithm + ")");
			if( certGen != null ) {
				Iterator<String> itr = certGen.getSignatureAlgNames();
				while( itr.hasNext() ) {
					String algName = itr.next();
					System.out.println( "Algorithm: " + algName + " " + (algName.equals(certificateAlgorithm)));
				}
			}
			nsa.printStackTrace();
			return false;
		} catch (InvalidKeyException ike) {
			EventLink.logger.log("Unable to generate key");
			ike.printStackTrace();
			return false;
		} catch (SignatureException se) {
			EventLink.logger.log("Signature error");
			se.printStackTrace();
			return false;
		} catch (CertificateEncodingException cee ) {
			EventLink.logger.log("Encoding error");
			cee.printStackTrace();
			return false;
		}

		return createKeyFile(file, password, keyPair, cert, forceWrite);


	}

	static boolean createKeyFile(File file, String password, KeyPair keyPair, Certificate cert, boolean force) {

		char[] passwordArray = password.toCharArray();

		KeyStore ks;

		try {
			ks = KeyStore.getInstance("JKS");
			ks.load(null, passwordArray);
		} catch (KeyStoreException e) {
			EventLink.logger.log("Keystore creation error");
			e.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		} catch (CertificateException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		if(keyPair != null && cert != null) {
			Certificate[] certs = new Certificate[1];
			certs[0] = cert;

			System.out.println( cert.toString() );

			try {
				ks.setKeyEntry("privateKeyAlias", keyPair.getPrivate(), passwordArray, certs);
			} catch (KeyStoreException e) {
				EventLink.logger.log("Failed to add keys to store");
				e.printStackTrace();
				return false;
			}
		}

		synchronized(fileLock) {
			FileOutputStream fos = null;
			try {
				if( force || !file.exists() ) {
					ks.store(fos = new FileOutputStream(file), passwordArray);
				}

			} catch (KeyStoreException e) {
				e.printStackTrace();
				return false;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return false;
			} catch (CertificateException e) {
				e.printStackTrace();
				return false;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				if(fos!=null) {
					try {
						fos.close();
					} catch (IOException e) {}
				}
			}
		}


		return true;

	}

	static KeyStore loadKeyStore(File file, String password) {
		char[] passwordArray = password.toCharArray();

		KeyStore ks = null;

		if( file == null || !file.exists() ) {
			return null;
		}

		FileInputStream fis = null;

		synchronized(fileLock) {
			try {
				ks = KeyStore.getInstance("JKS");
				fis = new FileInputStream(file);
				ks.load(fis, passwordArray);
			} catch (KeyStoreException e) {
				EventLink.logger.log("Keystore creation error");
				e.printStackTrace();
				return null;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			} catch (CertificateException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					if(fis!=null) {
						fis.close();
					}
				} catch (IOException e) {
					return null;
				}
			}
		}

		return ks;
	}

	static public Enumeration<String> getAliases(File file, String password) {

		KeyStore ks = loadKeyStore(file, password);

		try {
			return ks.aliases();
		} catch (KeyStoreException e) {
			return null;
		}

	}

	static public Certificate getCertificate(File file, String password) {

		KeyStore ks = loadKeyStore(file, password);
		if(ks==null) {
			return null;
		}

		Enumeration<String> aliases;

		try {
			aliases = ks.aliases();
			if(aliases.hasMoreElements()) {
				return ks.getCertificate(aliases.nextElement());
			} else {
				return null;
			}
		} catch (KeyStoreException e) {
			EventLink.logger.log("Unable to read alias list");
			return null;
		}


	}

	static public PrivateKey getPrivateKey(File file, String password) {

		KeyStore ks = loadKeyStore(file, password);
		if(ks==null) {
			return null;
		}

		char[] passwordArray = password.toCharArray();

		try {

			PrivateKey privateKey = null;

			try {
				privateKey = (PrivateKey)ks.getKey("privateKeyAlias", passwordArray);
			} catch (UnrecoverableKeyException e) {
				EventLink.logger.log("Unable to read keys from keystore");
				return null;
			} catch (NoSuchAlgorithmException e) {
				EventLink.logger.log("Unable to read keys from keystore (unknown algorithm)");
				e.printStackTrace();
				return null;
			}

			if(privateKey == null) {
				return null;
			}

			return privateKey;

		} catch (KeyStoreException e) {
			EventLink.logger.log("Unable to read alias list");
			return null;
		}


	}
	
	static String getFullAlias(KeyStore ks, String serverName) {
		
		Enumeration<String> aliases;
		
		try {
			aliases = ks.aliases();
		} catch (KeyStoreException e) {
			return null;
		}
		
		while(aliases.hasMoreElements()) {
			String current = aliases.nextElement();
			if(current.startsWith(serverName + ";")) {
				return current;
			}
		}
		
		return null;
		
	}
	
	static String removeCertificate(File file, String password, String serverName) {


		synchronized(fileLock) {

			KeyStore ks = loadKeyStore(file, password);

			if(ks==null) {
				return "Failed to load keystore";
			}
			
			String alias = getFullAlias(ks, serverName);
			if( alias == null ) {
				return serverName + " not found in keystore";
			}

			try {
				if( ks.getCertificate(alias) == null ) {
					return serverName + " not found in keystore";
				}
			} catch (KeyStoreException kse) {
				kse.printStackTrace();
				return "Error scanning keystore for cert";
			}


			try {
				ks.deleteEntry(alias);
			} catch (KeyStoreException e) {
				EventLink.logger.log("Failed to remove " + alias + " from store");
				e.printStackTrace();
				return "Error removing key from store";
			}
			
			return (saveKeyStore(ks, file, password))?(alias + " removed from keystore"):"Failed to save updated keystore";


		}

	}


	static boolean addCertificate(File file, String password, String alias, Certificate cert) {


		synchronized(fileLock) {

			KeyStore ks = loadKeyStore(file, password);

			if(ks==null) {
				return false;
			}

			try {
				if( ks.getCertificate(alias) != null ) {
					EventLink.logger.log("Certificate already exists for: " + alias );
					return false;
				}
			} catch (KeyStoreException kse) {
				kse.printStackTrace();
				return false;
			}


			try {
				ks.setCertificateEntry(alias, cert);
			} catch (KeyStoreException e) {
				EventLink.logger.log("Failed to add cert to store");
				e.printStackTrace();
				return false;
			}
			
			return saveKeyStore(ks, file, password);


		}

	}

	static boolean saveKeyStore(KeyStore ks, File file, String password) {

		char[] passwordArray = password.toCharArray();

		FileOutputStream out = null;

		synchronized(fileLock) {
			try {
				EventLink.logger.log("Saving certs to store: " + file);
				out = new FileOutputStream(file);
				ks.store(out, passwordArray);
			} catch (KeyStoreException e) {
				e.printStackTrace();
				return false;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return false;
			} catch (CertificateException e) {
				e.printStackTrace();
				return false;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				if(out!=null) {
					try {
						out.flush();
						out.close();
					} catch (IOException e) {
						return false;
					}
				}
			}
		}

		return true;

	}

	static public KeyManager[] getKeyManagers(File file, String password) {

		char[] passwordArray = password.toCharArray();

		KeyStore ks;

		if( file == null || !file.exists() ) {
			return null;
		}

		synchronized(fileLock) {
			FileInputStream fis = null;
			try {
				ks = KeyStore.getInstance("JKS");
				ks.load(fis = new FileInputStream(file), passwordArray);
			} catch (KeyStoreException e) {
				EventLink.logger.log("Keystore creation error");
				e.printStackTrace();
				return null;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			} catch (CertificateException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				if(fis!=null) {
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}
			}
		}

		KeyManagerFactory keyManagerFactory;
		try {
			keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(ks, passwordArray);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (UnrecoverableKeyException e) {
			return null;
		} catch (KeyStoreException e) {
			e.printStackTrace();
			return null;
		}

		return keyManagerFactory.getKeyManagers();

	}

	static SSLSocket getSSLSocket(String hostname, int portnum, KeyManager[] keyManagers, TrustManager trustManager) {

		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		try {
			sc.init(
					keyManagers,
					new TrustManager[] {trustManager},
					new SecureRandom());
		} catch (KeyManagementException e) {
			return null;
		}

		try {
			SSLSocket socket = (SSLSocket)sc.getSocketFactory().createSocket(hostname, portnum);

			return socket;
		} catch (UnknownHostException e) {
			return null;
		} catch (IOException e) {
			return null;
		}


	}

	static ServerSocket getSSLServerSocket (EventLink p, int portnum, KeyManager[] keyManagers, TrustManager trustManager) throws BindException {

		SSLContext sc;

		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		try {
			sc.init(
					keyManagers,
					new TrustManager[] {trustManager},
					new SecureRandom());
		} catch (KeyManagementException e) {
			p.log("Keymanager exception");
			return null;
		}

		ServerSocket socket = null;

		boolean success = false;

		try {
			socket = sc.getServerSocketFactory().createServerSocket(portnum);
			success = true;
		} catch (UnknownHostException e) {
			p.log("Unknown hostname starting server");
			return null;
		} catch (BindException be) {
			throw be;
		} catch (IOException e) {
			p.log("IO exception starting server");
			return null;
		} finally {
			if(!success) {
				if(socket!=null) {
					try {
						socket.close();
					} catch (IOException e) {}
					socket = null;
				}
			}
		}

		return socket;


	}

	static public String getHostname(String hostname) {
		String[] split = hostname.split(":");

		if(split.length>1) {
			return split[0];
		} else {
			return hostname;
		}
	}

	static public int getPortnum(String hostname) {
		String[] split = hostname.split(":");

		if(split.length>1) {
			try {
				return Integer.parseInt(split[1]);
			} catch (NumberFormatException nfe) {
				return 25365;
			}
		} else {
			return 25365;
		}
	}

	static void closeSocket(Socket s) {
		try {
			s.close();
		} catch (IOException ioe) {
		}
	}

}

