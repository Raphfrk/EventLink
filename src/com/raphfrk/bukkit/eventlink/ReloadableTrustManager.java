package com.raphfrk.bukkit.eventlink;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

class ReloadableTrustManager implements X509TrustManager  {

	Object syncObject = new Object();
	private X509TrustManager trustManager = null;

	public ReloadableTrustManager(File clientFile, String password) {
		reloadTrustStore(clientFile, password);
	}

	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
	throws CertificateException {
		synchronized(syncObject) {
			trustManager.checkClientTrusted(arg0, arg1);
		}

	}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
	throws CertificateException {
		synchronized(syncObject) {
			trustManager.checkServerTrusted(arg0, arg1);
		}

	}

	public X509Certificate[] getAcceptedIssuers() {
		synchronized(syncObject) {
			return trustManager.getAcceptedIssuers();
		}
	}

	X509TrustManager getTrustManager() {
		return trustManager;
	}

	boolean reloadTrustStore( File clientFile,  String password ) {

		synchronized(syncObject) {
			char[] passwordArray = password.toCharArray();

			KeyStore ks;

			if( clientFile == null || !clientFile.exists() ) {
				return false;
			}

			FileInputStream in = null;

			synchronized(SSLUtils.fileLock) {
				try {
					ks = KeyStore.getInstance("JKS");
					in = new FileInputStream(clientFile);
					ks.load(in, passwordArray);

				} catch (KeyStoreException e) {
					MiscUtils.log("Keystore creation error");
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
				} finally {
					try {
						if( in != null ) {
							in.close();
						}
					} catch (IOException e) {}
				}
			}

			TrustManagerFactory trustManagerFactory;
			try {
				trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			} catch (NoSuchAlgorithmException e1) {
				MiscUtils.log("trust store no such algorithm error on reload");
				e1.printStackTrace();
				return false;
			}

			try {
				trustManagerFactory.init(ks);
			} catch (KeyStoreException e) {
				MiscUtils.log("trust store init error on reload");
				e.printStackTrace();
				return false;
			}

			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

			for( TrustManager trustManager : trustManagers ) {
				if( trustManager instanceof X509TrustManager ) {
					this.trustManager = (X509TrustManager)trustManager;
					return true;
				}
			}

			return false;

		}

	}

}