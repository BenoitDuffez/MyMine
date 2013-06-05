package net.bicou.redmine.net.ssl;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * Allows you to trust certificates from additional KeyStores in addition to the default KeyStore
 */
public class MyMineSSLSocketFactory extends SSLSocketFactory {
	protected SSLContext sslContext = SSLContext.getInstance("TLS");
	TrustManager[] mTrustManagers;
	KeyManager[] mKeyManagers;

	public MyMineSSLSocketFactory(final KeyManager keyManager, final TrustManager trustManager) throws NoSuchAlgorithmException,
			KeyManagementException, KeyStoreException, UnrecoverableKeyException {
		super(null, null, null, null, null, null);
		if (keyManager != null) {
			mKeyManagers = new KeyManager[] {
				keyManager,
			};
		}
		if (trustManager != null) {
			mTrustManagers = new TrustManager[] {
				trustManager,
			};
		}
		sslContext.init(mKeyManagers, mTrustManagers, null);
	}

	@Override
	public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose) throws IOException {
		return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	}

	@Override
	public Socket createSocket() throws IOException {
		return sslContext.getSocketFactory().createSocket();
	}
}
