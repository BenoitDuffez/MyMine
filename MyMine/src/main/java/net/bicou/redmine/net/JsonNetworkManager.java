package net.bicou.redmine.net;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.bicou.redmine.R;
import net.bicou.redmine.app.ssl.SupportSSLKeyManager;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.CalendarDeserializer;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.json.VersionStatusDeserializer;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.net.ssl.MyMineSSLSocketFactory;
import net.bicou.redmine.net.ssl.MyMineSSLTrustManager;
import net.bicou.redmine.util.L;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.KeyManager;

/**
 * Base class for managing HTTP requests to the server.
 * <p/>
 * Derived class:
 * <ul>
 * <li><b>must</b> call one of {@link #init(android.content.Context, net.bicou.redmine.data.Server, String)}, {@link #init(android.content.Context,
 * net.bicou.redmine.data.Server, String, java.util.List)} or {@link #init(android.content.Context, net.bicou.redmine.data.Server, String,
 * org.apache.http.NameValuePair[])}  methods before doing anything.</li>
 * <li><b>must</b> call {@link #downloadJson()} in order to trigger the HTTP request and retrieve the response.</li>
 * <li>can override {@link #getHttpRequest()} in order to customize the HTTP request to be sent to the server.</li>
 * </ul>
 */
public abstract class JsonNetworkManager {
	/**
	 * The URI where we will send the HTTP request
	 */
	protected URI mURI;

	/**
	 * Used for SSL-related stuff
	 */
	Context mContext;

	/**
	 * Used to build the HTTP URI
	 */
	Server mServer;

	/**
	 * Relative part of the URI
	 */
	String mQueryPath;

	/**
	 * HTTP GET query arguments
	 */
	List<NameValuePair> mArgs;

	/**
	 * Used to create HTTPS (TLS) sockets
	 */
	protected MyMineSSLSocketFactory mSslSocketFactory;

	/**
	 * Used to handle self-signed certificates trustfulness
	 */
	protected MyMineSSLTrustManager mSSLTrustManager;

	/**
	 * Created when an error happens during the process
	 */
	protected JsonNetworkError mError;

	/**
	 * Set class members
	 *
	 * @param context   App context
	 * @param server    Redmine server
	 * @param queryPath Relative query path
	 */
	public void init(Context context, Server server, String queryPath) {
		init(context, server, queryPath, (List<NameValuePair>) null);
	}

	/**
	 * Set class members
	 *
	 * @param context   App context
	 * @param server    Redmine server
	 * @param queryPath Relative query path
	 * @param args      HTTP GET arguments
	 */
	public void init(Context context, Server server, String queryPath, List<NameValuePair> args) {
		mContext = context;
		mServer = server;
		mQueryPath = queryPath;
		mArgs = args;
	}

	/**
	 * Set class members
	 *
	 * @param context   App context
	 * @param server    Redmine server
	 * @param queryPath Relative query path
	 * @param args      HTTP GET arguments
	 */
	public void init(Context context, Server server, String queryPath, NameValuePair[] args) {
		ArrayList<NameValuePair> argsList = new ArrayList<NameValuePair>();
		Collections.addAll(argsList, args);
		init(context, server, queryPath, argsList);
	}

	private boolean mStripJsonContainer = false;

	/**
	 * Basic setter
	 *
	 * @param stripJsonContainer Whether the main JSON container should be stripped before any parsing attempt is made
	 */
	protected void setStripJsonContainer(final boolean stripJsonContainer) {
		mStripJsonContainer = stripJsonContainer;
	}

	/**
	 * Actually strip the JSON container
	 *
	 * @param json Input JSON string
	 *
	 * @return Output JSON string, stripped from its main container
	 */
	public String stripJsonContainer(final String json) {
		final int start = json.indexOf(":") + 1;
		final int end = json.lastIndexOf("}");
		return json.substring(start, end);
	}

	/**
	 * This code only wraps a Gson object with the required deserializers
	 *
	 * @param json Input JSON string
	 * @param type Output object type
	 *
	 * @return The object created by the Gson API
	 */
	public static <T> T gsonParse(String json, Class<T> type) {
		T object = null;

		try {
			final GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(Calendar.class, new CalendarDeserializer());
			builder.registerTypeAdapter(Version.VersionStatus.class, new VersionStatusDeserializer());
			final Gson gson = builder.create();

			object = gson.fromJson(json, type);
		} catch (final JsonSyntaxException e) {
			L.e("Unparseable JSON is:");
			L.e(json);
		} catch (final IllegalStateException e) {
			L.e("Unparseable JSON is:");
			L.e(json);
		} catch (final Exception e) {
			L.e("Unparseable JSON is:");
			L.e(json);
			L.e("Unable to parse JSON", e);
		}

		return object;
	}

	/**
	 * Getter
	 *
	 * @return The current error, if any
	 */
	public JsonNetworkError getError() {
		return mError;
	}

	/**
	 * Build the list of all HTTP GET arguments. Automatically add the API key
	 *
	 * @return The full list of HTTP GET arguments
	 */
	protected List<NameValuePair> buildUriArgs() {
		final List<NameValuePair> args = new ArrayList<NameValuePair>();
		if (mArgs != null) {
			args.addAll(mArgs);
		}
		args.add(new BasicNameValuePair("key", mServer.apiKey));
		return args;
	}

	/**
	 * Build the {@link #mURI} object
	 *
	 * @throws URISyntaxException as defined by {@link org.apache.http.client.utils.URIUtils#createURI(String, String, int, String, String, String)}
	 */
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
	 * Create the {@link net.bicou.redmine.net.ssl.MyMineSSLSocketFactory} that will provide our certificates to the HTTP client
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
	 * Prepare the Apache HttpClient object with correct SSL handling and Basic Authentication.
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

	/**
	 * Override this for custom GET/POST/etc HTTP queries
	 *
	 * @return The custom HTTP query, derived from {@link org.apache.http.client.methods.HttpRequestBase}
	 */
	protected HttpRequestBase getHttpRequest() {
		return new HttpGet(mURI);
	}

	/**
	 * Actually download the server response. In case of failure, an object will be stored in {@link #mError}.
	 *
	 * @return The server response, as a {@code String}
	 */
	protected String downloadJson() {
		BufferedReader reader = null;
		InputStream inputStream = null;
		final StringBuilder builder = new StringBuilder();
		String json = null;
		int statusCode = 0;

		try {
			buildURI();
			L.i("Loading JSON from: " + mURI);
			if (mURI == null) {
				return null;
			}

			final HttpClient httpClient = getHttpClient();
			final HttpRequestBase req = getHttpRequest();

			if (req == null) {
				mError = new JsonDownloadError(JsonDownloadError.ErrorType.TYPE_UNKNOWN);
				mError.httpResponseCode = 0;
				mError.setMessage(R.string.err_unknown);
				return null;
			}

			// Ask for UTF-8
			if (!req.containsHeader("Content-Type")) {
				req.setHeader("Content-Type", "application/json; charset=utf-8");
			}
			req.setHeader("Accept-Encoding", "gzip");

			// Execute request
			final HttpResponse resp = httpClient.execute(req);

			// Check result
			statusCode = resp.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
				L.d("Got HTTP " + statusCode + ": " + resp.getStatusLine().getReasonPhrase());
				mError = new JsonDownloadError(JsonDownloadError.ErrorType.TYPE_NETWORK);
				mError.httpResponseCode = statusCode;
				mError.setMessage(R.string.err_http, "HTTP " + statusCode + ": " + resp.getStatusLine().getReasonPhrase());

				// Special case for HTTP 422: it contains a content
				if (statusCode != HttpStatus.SC_UNPROCESSABLE_ENTITY) {
					return null;
				}
			}

			// Handle proper incoming encoding (GZIP?)
			HttpEntity entity = resp.getEntity();
			Header contentEncoding = resp.getFirstHeader("Content-Encoding");
			String encoding = contentEncoding == null ? null : contentEncoding.getValue();
			if (!TextUtils.isEmpty(encoding) && encoding.equalsIgnoreCase("gzip")) {
				inputStream = new GZIPInputStream(entity.getContent());
			} else {
				inputStream = entity.getContent();
			}

			// Handle the incoming charset
			Header contentType = resp.getFirstHeader("Content-Type");
			String charset = contentType.getValue();
			if (charset.contains("charset=")) {
				charset = charset.substring(charset.indexOf("charset=") + "charset=".length());
			} else {
				charset = "UTF-8";
			}

			// Read response
			reader = new BufferedReader(new InputStreamReader(inputStream, charset));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			json = builder.toString();
		} catch (final Exception e) {
			L.e("Unable to download  from " + mURI + " because:" + e.toString());
			mError = new JsonDownloadError(JsonDownloadError.ErrorType.TYPE_NETWORK, e);
			if (mSslSocketFactory != null) {
				mError.chain = mSSLTrustManager.getServerCertificates();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Edit json? (only if valid response)
		if (mStripJsonContainer && statusCode >= 200 && statusCode < 300) {
			L.d("The JSON was striped from its main container");
			json = stripJsonContainer(json);
		}

		return json;
	}
}
