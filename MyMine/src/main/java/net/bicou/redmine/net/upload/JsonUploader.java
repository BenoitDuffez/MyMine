package net.bicou.redmine.net.upload;

import android.content.Context;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.net.JsonNetworkManager;
import net.bicou.redmine.util.L;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

public class JsonUploader extends JsonNetworkManager {
	ObjectSerializer mObjectSerializer;
	ObjectSerializer.RemoteOperation mRemoteOperation;

	public JsonUploader() {
	}

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
			return mError;
		}

		return json;
	}

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
				entity = new StringEntity(mObjectSerializer.convertToJson(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				L.e("Couldn't send the JSON string as UTF-8!", e);
				throw new IllegalStateException(e);
			}
			entity.setContentEncoding("UTF-8");
			((HttpEntityEnclosingRequestBase) request).setEntity(entity);
		}
		return request;
	}
}
