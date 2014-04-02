package net.bicou.redmine.net;

import android.content.Context;
import android.text.TextUtils;

import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.AbsObjectList;
import net.bicou.redmine.net.JsonDownloadError.ErrorType;
import net.bicou.redmine.util.L;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;

public class JsonDownloader<T> extends JsonNetworkManager {
	T mObject;
	Class<T> mType;
	private boolean mDownloadAllIfList = true;
	int mCurrentOffset;

	public JsonDownloader(final Class<T> type) {
		mType = type;
	}

	/**
	 * Prevent all objects from being downloaded if the remote JSON contains a list of objects (see {@link AbsObjectList})
	 */
	public JsonDownloader<T> setDownloadAllIfList(final boolean downloadAllIfList) {
		mDownloadAllIfList = downloadAllIfList;
		return this;
	}

	public JsonDownloader<T> stripJsonContainer(final boolean stripJsonContainer) {
		super.setStripJsonContainer(stripJsonContainer);
		return this;
	}

	@Override
	protected List<NameValuePair> buildUriArgs() {
		List<NameValuePair> args = super.buildUriArgs();
		boolean addOffset = true;
		for (final NameValuePair arg : args) {
			if ("offset".equals(arg.getName())) {
				addOffset = false;
			}
		}
		if (addOffset && mCurrentOffset > 0) {
			args.add(new BasicNameValuePair("offset", Integer.toString(mCurrentOffset)));
		}

		return args;
	}

	/**
	 * Downloads a json object and convert it to a java object
	 *
	 * @param ctx    Required to retrieve the SSL certificates from the app keystore or Android's
	 * @param server Target server
	 * @param uri    Target URI
	 */
	public T fetchObject(final Context ctx, final Server server, final String uri) {
		init(ctx, server, uri);
		return downloadAndParse();
	}

	/**
	 * Downloads a json object and convert it to a java object
	 *
	 * @param ctx    Required to retrieve the SSL certificates from the app keystore or Android's
	 * @param server Target server
	 * @param uri    Target URI
	 * @param args   Optional HTTP GET arguments
	 */
	public T fetchObject(final Context ctx, final Server server, final String uri, final NameValuePair[] args) {
		init(ctx, server, uri, args);
		return downloadAndParse();
	}

	/**
	 * Downloads a json object and convert it to a java object
	 *
	 * @param ctx    Required to retrieve the SSL certificates from the app keystore or Android's
	 * @param server Target server
	 * @param uri    Target URI
	 * @param args   Optional HTTP GET arguments
	 */
	public T fetchObject(final Context ctx, final Server server, final String uri, final List<NameValuePair> args) {
		init(ctx, server, uri, args);
		return downloadAndParse();
	}

	@SuppressWarnings("unchecked")
	private T downloadAllObjects() {
		String json;
		try {
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
					if (mObjectAsList != null) {
						mObjectAsList.addObjects(object.getObjects());
					}
				} else {
					break;
				}

				if (!mDownloadAllIfList) {
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

		return mObject;
	}

	private T downloadAndParse() {
		if (AbsObjectList.class.isAssignableFrom(mType)) {
			return downloadAllObjects();
		} else {
			final String json = downloadJson();
			if (mError != null) {
				return null;
			}
			return parseJson(json);
		}
	}

	@SuppressWarnings("unchecked")
	private T getHtml(String json) {
		return (T) json;
	}

	private T parseJson(String json) {
		if (mType.equals(String.class)) {
			return getHtml(json);
		}

		if (TextUtils.isEmpty(json)) {
			L.e("Received JSON for " + mType.toString() + " from " + mURI + " is empty");
			mError = new JsonDownloadError(ErrorType.TYPE_RESPONSE);
			mError.setMessage(R.string.err_empty_response);
			return null;
		}

		// Edit json?
		if (mStripJsonContainer) {
			L.d("The JSON was striped from its main container");
			json = stripJsonContainer(json);
		}

		T object = gsonParse(json, mType);

		if (object == null) {
			mError = new JsonDownloadError(ErrorType.TYPE_JSON);
			mError.setMessage(R.string.err_parse_error);
		}

		return object;
	}
}
