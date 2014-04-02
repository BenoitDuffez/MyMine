package net.bicou.redmine.net.upload;

import android.content.Context;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.net.JsonNetworkManager;
import net.bicou.redmine.util.L;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

/**
 * Class used to upload data to the server, as JSON object strings. Can be used to upload a new object, update or delete an existing one.
 * <p/>
 * Usage:
 * <pre>
 * // Create a serializer (that will convert an object to a JSON string, using the base class {@link net.bicou.redmine.net.upload.ObjectSerializer})
 * IssueSerializer serializer = new IssueSerializer(applicationContext, issue, null, true);
 * // Build it
 * serializer.build();
 * // Create the relative part of the HTTP query
 * String uri="issues/" + issue.id + ".json";
 * // Actually upload
 * result = new JsonUploader().uploadObject(applicationContext, issue.server, uri, serializer);
 * </pre>
 */
public class JsonUploader extends JsonNetworkManager {
	ObjectSerializer mObjectSerializer;
	ObjectSerializer.RemoteOperation mRemoteOperation;

	public JsonUploader() {
	}

	/**
	 * Actually upload an object to the server. The object is contained in the serializer, which will generate the JSON string to upload.
	 *
	 * @param context   Application context
	 * @param server    The Redmine server
	 * @param queryPath The relative HTTP query part
	 * @param object    The {@link net.bicou.redmine.net.upload.ObjectSerializer}
	 *
	 * @return The JSON string returned by the server in case of success, or a {@link net.bicou.redmine.net.JsonNetworkError} in case of failure.
	 */
	public Object uploadObject(Context context, Server server, String queryPath, ObjectSerializer object) {
		mObjectSerializer = object;

		mRemoteOperation = mObjectSerializer.getRemoteOperation();
		if (mRemoteOperation == ObjectSerializer.RemoteOperation.NO_OP) {
			L.i("Will not upload " + mObjectSerializer);
			return null;
		}

		L.i("Uploading " + mObjectSerializer + " to server: " + server + ", with uri: " + queryPath);
		init(context, server, queryPath);

		String json = downloadJson();
		if (mError != null) {
			mError.json = json;
			return mError;
		}

		return json;
	}

	/**
	 * Custom HTTP request maker with HTTP POST/PUT/DELETE depending on the desired action.
	 *
	 * @return A new instance of a {@link org.apache.http.client.methods.HttpRequestBase}
	 */
	@Override
	protected HttpRequestBase getHttpRequest() {
		ObjectSerializer.RemoteOperation op = mObjectSerializer.getRemoteOperation();
		HttpRequestBase request;
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

		if (op != ObjectSerializer.RemoteOperation.DELETE) {
			StringEntity entity;
			try {
				String rawJson = mObjectSerializer.convertToJson();
				rawJson = rawJson.replace("\r", "");
				entity = new StringEntity(rawJson, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				L.e("Couldn't send the JSON string as UTF-8!", e);
				throw new IllegalStateException(e);
			}
			entity.setContentEncoding("UTF-8");
			//noinspection ConstantConditions
			((HttpEntityEnclosingRequestBase) request).setEntity(entity);
		}
		return request;
	}
}
