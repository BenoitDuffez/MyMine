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
import net.bicou.redmine.data.json.AbsObjectList;
import net.bicou.redmine.data.json.CalendarDeserializer;
import net.bicou.redmine.data.json.Version.VersionStatus;
import net.bicou.redmine.data.json.VersionStatusDeserializer;
import net.bicou.redmine.net.JsonDownloadError.ErrorType;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.net.ssl.MyMineSSLSocketFactory;
import net.bicou.redmine.net.ssl.MyMineSSLTrustManager;
import net.bicou.redmine.util.L;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import java.util.List;
import java.util.zip.GZIPInputStream;

public class JsonDownloader<T> {
	T mObject;
	Class<T> mType;
	URI mURI;
	long mRowId;
	private boolean mDownloadAllIfList = true;
	boolean mStripJsonContainer = false;

	/**
	 * Will contain information regarding the failure, if any. See {@link JsonDownloadError}.
	 */
	JsonDownloadError mError = null;

	Context mContext;

	Server mServer;
	String mQueryPath;
	List<NameValuePair> mArgs;
	int mCurrentOffset;

	MyMineSSLSocketFactory mSslSocketFactory;
	MyMineSSLTrustManager mSSLTrustManager;

	/**
	 * This constructor can only be used if no callback is used, i.e. when the task is executed through {@code #syncExecute()}
	 *
	 * @param type
	 */
	public JsonDownloader(final Class<T> type) {
		mType = type;
	}

	/**
	 * Prevent all objects from being downloaded if the remote JSON contains a list of objects (see {@link AbsObjectList})
	 *
	 * @param downloadAllIfList
	 * @return
	 */
	public JsonDownloader<T> setDownloadAllIfList(final boolean downloadAllIfList) {
		mDownloadAllIfList = downloadAllIfList;
		return this;
	}

	public JsonDownloader<T> setStripJsonContainer(final boolean stripJsonContainer) {
		mStripJsonContainer = stripJsonContainer;
		return this;
	}

	public JsonDownloadError getError() {
		return mError;
	}

	public String stripJsonContainer(final String json) {
		final int start = json.indexOf(":") + 1;
		final int end = json.lastIndexOf("}");
		return json.substring(start, end);
	}

	private void buildURI() throws URISyntaxException {
		final Uri uri = Uri.parse(mServer.serverUrl);

		final List<NameValuePair> args = new ArrayList<NameValuePair>();
		boolean addOffset = true;
		if (mArgs != null) {
			for (final NameValuePair arg : mArgs) {
				if ("offset".equals(arg.getName())) {
					addOffset = false;
				}
				args.add(arg);
			}
		}
		if (addOffset && mCurrentOffset > 0) {
			args.add(new BasicNameValuePair("offset", Integer.toString(mCurrentOffset)));
		}
		args.add(new BasicNameValuePair("key", mServer.apiKey));

		String path = uri.getPath();
		if (!path.endsWith("/")) {
			path += "/";
		}
		path += mQueryPath;

		mURI = URIUtils.createURI(uri.getScheme(), uri.getHost(), uri.getPort(), path, URLEncodedUtils.format(args, "UTF-8"), null);
	}

	public T fetchObject(final Context ctx, final Server server, final String uri) {
		mContext = ctx;
		mServer = server;
		mQueryPath = uri;
		return downloadAndParse();
	}

	public T fetchObject(final Context ctx, final Server server, final String uri, final NameValuePair[] args) {
		mContext = ctx;
		mServer = server;
		mQueryPath = uri;
		mArgs = new ArrayList<NameValuePair>();
		for (final NameValuePair arg : args) {
			mArgs.add(arg);
		}

		return downloadAndParse();
	}

	public T fetchObject(final Context ctx, final Server server, final String uri, final List<NameValuePair> args) {
		mContext = ctx;
		mServer = server;
		mQueryPath = uri;
		mArgs = args;

		return downloadAndParse();
	}

	@SuppressWarnings("unchecked")
	private T downloadAndParse() {
		if (AbsObjectList.class.isAssignableFrom(mType)) {
			String json;
			try {
				// T object = mType.newInstance();
				int downloadedObjects = 0, limit = 0, total = 0;
				AbsObjectList<T> mObjectAsList = null, object;
				mCurrentOffset = 0;

				do {
					json = downloadJson();
					if (mError != null) {
						return null;
					}
					object = (AbsObjectList<T>) parseJson(json);

					if (object != null) {
						limit = object.limit;
						total = object.total_count;

						mCurrentOffset += limit;

						if (mObject == null) {
							mObject = mType.newInstance();
							mObjectAsList = (AbsObjectList<T>) mObject;
							mObjectAsList.init(object);
						}

						downloadedObjects += object.getSize();
						mObjectAsList.addObjects(object.getObjects());
					} else {
						break;
					}

					if (mDownloadAllIfList == false) {
						break;
					}
				} while (downloadedObjects < total);

				if (mObjectAsList != null) {
					mObjectAsList.downloadedObjects = downloadedObjects;
					mObjectAsList.total_count = total;
					mObjectAsList.limit = limit;
					mObjectAsList.offset = 0;
				}
			} catch (final InstantiationException e) {
				L.e("unable to instantiate object", e);
				mError = new JsonDownloadError(ErrorType.TYPE_ANDROID);
				mError.setMessage(R.string.err_unable_to_instantiate_object, e.getMessage());
			} catch (final IllegalAccessException e) {
				L.e("unable to instantiate object", e);
				mError = new JsonDownloadError(ErrorType.TYPE_ANDROID);
				mError.setMessage(R.string.err_unable_to_instantiate_object, e.getMessage());
			}
		} else {
			final String json = downloadJson();
			mObject = parseJson(json);
		}

		return mObject;
	}

	/**
	 * Creates the {@link MyMineSSLSocketFactory} that will provide our certificates to the HTTP client
	 *
	 * @return
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
	 *
	 * @return
	 */
	private HttpClient getHttpClient() {
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
	 * Actually downloads the JSON from the server. In case of failure, an object will be stored in {@link JsonDownloader#mError}.
	 *
	 * @param offset
	 *            The current offset, if downloading many objects from a loop (to be deprecated?)
	 * @return the JSON, as a {@code String}
	 */
	private String downloadJson() {
		BufferedReader reader = null;
		InputStream inputStream = null;
		final StringBuilder builder = new StringBuilder();

		try {
			buildURI();
			L.i("Loading JSON from: " + mURI);
			if (mURI == null) {
				return null;
			}

			final HttpClient httpClient = getHttpClient();
			final HttpGet get = new HttpGet(mURI);
			final HttpResponse resp = httpClient.execute(get);

			// Ask for UTF-8
			get.setHeader("Content-Type", "application/json; charset=utf-8");

			if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				L.d("Got HTTP " + resp.getStatusLine().getStatusCode() + ": " + resp.getStatusLine().getReasonPhrase());
				mError = new JsonDownloadError(ErrorType.TYPE_NETWORK);
				mError.setMessage(R.string.err_http, "HTTP " + resp.getStatusLine().getStatusCode() + ": " + resp.getStatusLine().getReasonPhrase());
				return null;
			}

			// Handle proper incoming charset
			HttpEntity entity = resp.getEntity();
			String charset = entity.getContentEncoding() == null ? null : entity.getContentEncoding().getValue();
			if (TextUtils.isEmpty(charset)) {
				charset = "UTF-8";
			}

			// Handle gzip decompression
			if (charset.equalsIgnoreCase("gzip")) {
				inputStream = new GZIPInputStream(entity.getContent());
			} else {
				inputStream = entity.getContent();
			}

			// Read response
			reader = new BufferedReader(new InputStreamReader(inputStream, charset));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
		} catch (final Exception e) {
			L.e("Unable to download " + mType.toString() + " from " + mURI + " because:" + e.toString());
			mError = new JsonDownloadError(ErrorType.TYPE_NETWORK, e);
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

		return builder.toString();
	}

	private T parseJson(String json) {
		if (TextUtils.isEmpty(json)) {
			L.e("Received JSON for " + mType.toString() + " from " + mURI + " is empty");
			mError = new JsonDownloadError(ErrorType.TYPE_RESPONSE);
			mError.setMessage(R.string.err_empty_response);
			return null;
		}

		// Edit json?
		if (mStripJsonContainer) {
			L.d("The JSON was altererd");
			json = stripJsonContainer(json);
		}

		T object = null;

		try {
			final GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(Calendar.class, new CalendarDeserializer());
			builder.registerTypeAdapter(VersionStatus.class, new VersionStatusDeserializer());
			final Gson gson = builder.create();

			object = gson.fromJson(json, mType);
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

		if (object == null) {
			mError = new JsonDownloadError(ErrorType.TYPE_JSON);
			mError.setMessage(R.string.err_parse_error);
		}

		return object;
	}
}
