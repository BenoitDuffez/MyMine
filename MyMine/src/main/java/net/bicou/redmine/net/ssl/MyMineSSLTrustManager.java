package net.bicou.redmine.net.ssl;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import net.bicou.redmine.util.L;

/**
 * Based on http://download.oracle.com/javase/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager
 */
public class MyMineSSLTrustManager implements X509TrustManager {
	X509Certificate[] mCertificatesChain;
	protected ArrayList<X509TrustManager> x509TrustManagers = new ArrayList<X509TrustManager>();

	public MyMineSSLTrustManager(final KeyStore... additionalkeyStores) {
		final ArrayList<TrustManagerFactory> factories = new ArrayList<TrustManagerFactory>();

		try {
			// The default Trustmanager with default keystore
			final TrustManagerFactory original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			original.init((KeyStore) null);
			factories.add(original);

			if (additionalkeyStores != null && additionalkeyStores.length > 0) {
				for (final KeyStore keyStore : additionalkeyStores) {
					final TrustManagerFactory additionalCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					additionalCerts.init(keyStore);
					factories.add(additionalCerts);
				}
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		/*
		 * Iterate over the returned trustmanagers, and hold on to any that are X509TrustManagers
		 */
		for (final TrustManagerFactory tmf : factories) {
			for (final TrustManager tm : tmf.getTrustManagers()) {
				if (tm instanceof X509TrustManager) {
					x509TrustManagers.add((X509TrustManager) tm);
				}
			}
		}

		if (x509TrustManagers.size() == 0) {
			throw new RuntimeException("Couldn't find any X509TrustManagers");
		}
	}

	/*
	 * Delegate to the default trust manager.
	 */
	@Override
	public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
		final X509TrustManager defaultX509TrustManager = x509TrustManagers.get(0);
		defaultX509TrustManager.checkClientTrusted(chain, authType);
	}

	/*
	 * Loop over the trustmanagers until we find one that accepts our server
	 */
	@Override
	public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
		mCertificatesChain = chain;
		for (final X509TrustManager tm : x509TrustManagers) {
			try {
				tm.checkServerTrusted(chain, authType);
				return;
			} catch (final CertificateException e) {
				// ignore
			}
		}
		L.d("didn't find any trust manager");
		throw new CertificateException("Didn't find any TrustManager that would trust the server certificate");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		final ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
		for (final X509TrustManager tm : x509TrustManagers) {
			list.addAll(Arrays.asList(tm.getAcceptedIssuers()));
		}
		return list.toArray(new X509Certificate[list.size()]);
	}

	public X509Certificate[] getServerCertificates() {
		return mCertificatesChain;
	}
}
