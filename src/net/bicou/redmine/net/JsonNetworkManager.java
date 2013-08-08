package net.bicou.redmine.net;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import net.bicou.redmine.app.ssl.SupportSSLKeyManager;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.net.ssl.MyMineSSLSocketFactory;
import net.bicou.redmine.net.ssl.MyMineSSLTrustManager;
import net.bicou.redmine.util.L;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.KeyManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonNetworkManager {
	URI mURI;
	Context mContext;

	Server mServer;
	String mQueryPath;
	List<NameValuePair> mArgs;

	MyMineSSLSocketFactory mSslSocketFactory;
	MyMineSSLTrustManager mSSLTrustManager;
	protected JsonNetworkError mError;

	public JsonNetworkError getError() {
		return mError;
	}

	protected List<NameValuePair> buildUriArgs() {
		final List<NameValuePair> args = new ArrayList<NameValuePair>();
		if (mArgs != null) {
			args.addAll(mArgs);
		}
		args.add(new BasicNameValuePair("key", mServer.apiKey));
		return args;
	}

	protected void buildURI() throws URISyntaxException {
		final Uri uri = Uri.parse(mServer.serverUrl);

		final List<NameValuePair> args = buildUriArgs();

		String path = uri.getPath();
		if (TextUtils.isEmpty(path)) {
			path = "/";
		} else if (!path.endsWith("/")) {
			path += "/";
		}
		path += mQueryPath;

		mURI = URIUtils.createURI(uri.getScheme(), uri.getHost(), uri.getPort(), path, URLEncodedUtils.format(args, "UTF-8"), null);
	}

	/**
	 * Creates the {@link net.bicou.redmine.net.ssl.MyMineSSLSocketFactory} that will provide our certificates to the HTTP client
	 */
	protected MyMineSSLSocketFactory createAdditionalCertsSSLSocketFactory() {
		try {
			mSSLTrustManager = new MyMineSSLTrustManager(new KeyStoreDiskStorage(mContext).loadAppKeyStore());
			final KeyManager keyManager = SupportSSLKeyManager.init(mContext);
			return new MyMineSSLSocketFactory(keyManager, mSSLTrustManager);
		} catch (final KeyManagementException e) {
			L.e("Unable to create SSL factory!", e);
		} catch (final UnrecoverableKeyException e) {
			L.e("Unable to create SSL factory!", e);
		} catch (final NoSuchAlgorithmException e) {
			L.e("Unable to create SSL factory!", e);
		} catch (final KeyStoreException e) {
			L.e("Unable to create SSL factory!", e);
		}
		return null;
	}

	/**
	 * Prepares the Apache HttpClient object with correct SSL handling and Basic Authentication
	 */
	protected HttpClient getHttpClient() {
		final SchemeRegistry schemeRegistry = new SchemeRegistry();
		final int port = mURI.getPort();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), port > 0 ? port : 80));

		if ("https".equals(mURI.getScheme())) {
			mSslSocketFactory = createAdditionalCertsSSLSocketFactory();
			schemeRegistry.register(new Scheme("https", mSslSocketFactory, port > 0 ? port : 443));
		}

		final HttpParams params = new BasicHttpParams();
		final SingleClientConnManager cm = new SingleClientConnManager(params, schemeRegistry);
		final DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);

		// HTTP auth
		if (!TextUtils.isEmpty(mServer.authUsername)) {
			L.d("Authenticating " + mServer.authUsername + " with a password");
			final Credentials credentials = new UsernamePasswordCredentials(mServer.authUsername, mServer.authPassword);
			httpClient.getCredentialsProvider().setCredentials(new AuthScope(mURI.getHost(), mURI.getPort()), credentials);
		}

		return httpClient;
	}
}
