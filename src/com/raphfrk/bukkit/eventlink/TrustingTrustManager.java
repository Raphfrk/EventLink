package com.raphfrk.bukkit.eventlink;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


public class TrustingTrustManager implements X509TrustManager {

	public void checkClientTrusted(
			X509Certificate[] paramArrayOfX509Certificate, String paramString)
	throws CertificateException {
	}

	public void checkServerTrusted(
			X509Certificate[] paramArrayOfX509Certificate, String paramString)
	throws CertificateException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}


}