package net.bicou.redmine.net;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;

import java.security.cert.X509Certificate;

/**
 * Created by bicou on 08/08/13.
 */
public abstract class JsonNetworkError {
	// The error must be accessed through getMessage()
	private String mErrorMessage;
	private int errorMessageResId;
	public Exception exception;
	public X509Certificate[] chain;
	public int httpResponseCode;
	public String json;

	public abstract void displayCrouton(Activity activity, ViewGroup viewGroup);

	public JsonNetworkError() {

	}

	public JsonNetworkError(Exception e) {
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
}
