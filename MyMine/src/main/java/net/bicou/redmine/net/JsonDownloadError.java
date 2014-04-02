package net.bicou.redmine.net;

import android.app.Activity;
import android.text.TextUtils;
import android.view.ViewGroup;

import net.bicou.redmine.R;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Container for an useful explanation on the failure. Can be JSON parsing-related, network-related, unexpected response, invalid Android behavior or unknown. If
 * there is an exception raised, it is stored into {@link JsonDownloadError#exception}. If the error is SSL-based, the untrusted certificate chain will be stored
 * into {@link JsonDownloadError#chain}. The HTTP response code is also stored into {@link JsonDownloadError#httpResponseCode}.
 *
 * @author bicou
 */
public class JsonDownloadError extends JsonNetworkError {
	public static enum ErrorType {
		TYPE_JSON,
		TYPE_NETWORK,
		TYPE_RESPONSE,
		TYPE_ANDROID,
		TYPE_UNKNOWN,
	}

	public JsonDownloadError.ErrorType errorType;

	public JsonDownloadError(final JsonDownloadError.ErrorType type) {
		errorType = type;
	}

	public JsonDownloadError(final JsonDownloadError.ErrorType type, final Exception e) {
		super(e);
		errorType = type;
	}

	@Override
	public void displayCrouton(Activity activity, ViewGroup viewGroup) {
		if (activity == null) {
			return;
		}

		final String message = getMessage(activity);
		Crouton.makeText(activity, message, Style.ALERT, viewGroup).show();
	}

	@Override
	public void displayCrouton(Activity activity, int viewGroup) {
		if (activity == null) {
			return;
		}

		final String message = getErrorMessage(activity);
		Crouton.makeText(activity, message, Style.ALERT, viewGroup).show();
	}

	private String getErrorMessage(Activity activity) {
		final String message;
		switch (errorType) {
		case TYPE_NETWORK:
			final String details = getMessage(activity);
			if (TextUtils.isEmpty(details)) {
				message = activity.getString(R.string.err_http_nodetails);
			} else {
				message = activity.getString(R.string.err_http, details);
			}
			break;

		case TYPE_ANDROID:
			message = activity.getString(R.string.err_unable_to_instantiate_object);
			break;

		case TYPE_JSON:
			message = activity.getString(R.string.err_parse_error);
			break;

		case TYPE_RESPONSE:
			message = activity.getString(R.string.err_empty_response);
			break;

		default:
		case TYPE_UNKNOWN:
			message = activity.getString(R.string.err_unknown);
			break;
		}

		return message;
	}
}
