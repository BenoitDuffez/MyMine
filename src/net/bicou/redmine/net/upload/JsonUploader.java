package net.bicou.redmine.net.upload;

import android.content.Context;
import android.text.TextUtils;
import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.net.JsonDownloadError;
import net.bicou.redmine.net.JsonDownloadError.ErrorType;
import net.bicou.redmine.net.JsonNetworkManager;
import net.bicou.redmine.util.L;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class JsonUploader extends JsonNetworkManager {
	ObjectSerializer mObjectSerializer;
	ObjectSerializer.RemoteOperation mRemoteOperation;

	/**
	 * This constructor can only be used if no callback is used, i.e. when the task is executed through {@code #syncExecute()}
	 */
	public JsonUploader() {
	}

	public JsonUploadError uploadObject(Context context, Server s, String queryPath, ObjectSerializer object) {
		mObjectSerializer = object;

		mRemoteOperation = mObjectSerializer.getRemoteOperation();
		if (mRemoteOperation == ObjectSerializer.RemoteOperation.NO_OP) {
			return null;
		}

		L.d("Wanted to upload to server: " + s + ", with uri: " + queryPath);
		queryPath = "mymine.php";

		Server server = new Server("http://bicou.net/", s.apiKey);
		server.rowId = s.rowId;
		server.authUsername = s.authUsername;
		server.authPassword = s.authPassword;
		server.user = s.user;

		L.i("Will upload to server: " + server + ", with uri: " + queryPath + " instead");
		init(context, server, queryPath);
		return uploadJson();
	}

	/**
	 * Actually uploads the JSON from the server
	 *
	 * @return the error, if any
	 */
	private JsonUploadError uploadJson() {
		BufferedReader reader = null;
		InputStream inputStream = null;
		final StringBuilder builder = new StringBuilder();

		ObjectSerializer.RemoteOperation op = mObjectSerializer.getRemoteOperation();

		try {
			buildURI();
			L.i("Uploading JSON to: " + mURI);
			if (mURI == null) {
				return null;
			}

			final HttpClient httpClient = getHttpClient();
			final HttpRequestBase request;

			switch (op) {
			case DELETE:
				request = new HttpDelete(mURI);
				break;
			case ADD:
				request = new HttpPost(mURI);
				break;
			case EDIT:
				request = new HttpPut(mURI);
				break;

			default:
				throw new IllegalArgumentException("Invalid HTTP request");
			}

			// Ask for UTF-8
			request.setHeader("Content-Type", "application/json; charset=utf-8");
			request.setHeader("Accept-Encoding", "gzip");

			if (op != ObjectSerializer.RemoteOperation.DELETE) {
				StringEntity entity = new StringEntity(mObjectSerializer.convertToJson(), "UTF-8");
				entity.setContentEncoding("UTF-8");
				((HttpEntityEnclosingRequestBase) request).setEntity(entity);
			}

			final HttpResponse resp = httpClient.execute(request);

			if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				L.d("Got HTTP " + resp.getStatusLine().getStatusCode() + ": " + resp.getStatusLine().getReasonPhrase());
				mError = new JsonDownloadError(ErrorType.TYPE_NETWORK);
				mError.setMessage(R.string.err_http, "HTTP " + resp.getStatusLine().getStatusCode() + ": " + resp.getStatusLine().getReasonPhrase());
				return null;
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
		} catch (final Exception e) {
			L.e("Unable to upload " + mObjectSerializer + " to " + mURI, e);
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
		L.d("Received from server: " + builder.toString());
		return null;
	}
}
