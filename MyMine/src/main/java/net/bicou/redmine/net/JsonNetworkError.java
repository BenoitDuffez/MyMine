package net.bicou.redmine.net;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;

import java.security.cert.X509Certificate;

/**
 * Base class describing an error related to JSON download/parse.
 * Contains useful items for describing the error to the user
 * Created by bicou on 08/08/13.
 */
public abstract class JsonNetworkError {
    public Exception exception;
    public X509Certificate[] chain;
    public int httpResponseCode;
    public String json;
    // The error must be accessed through getMessage()
    private String mErrorMessage;
    private int errorMessageResId;

    public JsonNetworkError() {
    }

    public JsonNetworkError(Exception e) {
        exception = e;
    }

    public abstract void displayCrouton(Activity activity, ViewGroup viewGroup);

    public abstract void displayCrouton(Activity activity, int viewGroup);

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

    /**
     * This method will provide details regarding the error that happened
     *
     * @param ctx Used to get strings from resources
     * @return Error details, if possible
     */
    public
    @android.support.annotation.Nullable
    String getMessage(final Context ctx) {
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

    public String toString() {
        return getClass().getSimpleName() + " { message: " + mErrorMessage + ", exception: " + exception + ", HTTP response: " + httpResponseCode + ", json: " + json + " }";
    }
}
