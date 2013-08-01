package net.bicou.redmine.net;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.R;

import java.security.cert.X509Certificate;

/**
 * Container for an useful explanation on the failure. Can be JSON parsing-related, network-related, unexpected response, invalid Android behavior or unknown. If
 * there is an exception raised, it is stored into {@link JsonDownloadError#exception}. If the error is SSL-based, the untrusted certificate chain will be stored
 * into {@link JsonDownloadError#chain}. The HTTP response code is also stored into {@link JsonDownloadError#httpResponseCode}.
 *
 * @author bicou
 */
public class JsonDownloadError {
	public static enum ErrorType {
		TYPE_JSON,
		TYPE_NETWORK,
		TYPE_RESPONSE,
		TYPE_ANDROID,
		TYPE_UNKNOWN,
	}

	;

	// The error must be accessed through getMessage()
	private String mErrorMessage;
	private int errorMessageResId;

	// These are public
	public JsonDownloadError.ErrorType errorType;
	public Exception exception;
	public X509Certificate[] chain;
	public int httpResponseCode;

	public JsonDownloadError(final JsonDownloadError.ErrorType type) {
		errorType = type;
	}

	public JsonDownloadError(final JsonDownloadError.ErrorType type, final Exception e) {
		errorType = type;
		exception = e;
	}

	public void setMessage(final int resId, final String text) {
		mErrorMessage = text;
		errorMessageResId = resId;
	}

	public void setMessage(final String text) {
		mErrorMessage = text;
	}

	public void setMessage(final int resId) {
		errorMessageResId = resId;
	}

	public String getMessage(final Context ctx) {
		if (errorMessageResId > 0) {
			if (TextUtils.isEmpty(mErrorMessage)) {
				return ctx.getString(errorMessageResId);
			} else {
				return ctx.getString(errorMessageResId, mErrorMessage);
			}
		} else {
			return mErrorMessage;
		}
	}

	public void displayCrouton(Activity activity, ViewGroup viewGroup) {
		final String message;
		switch (errorType) {
		case TYPE_NETWORK:
			message = activity.getString(R.string.err_http, getMessage(activity));
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

		Crouton.makeText(activity, message, Style.ALERT, viewGroup).show();
	}
}
