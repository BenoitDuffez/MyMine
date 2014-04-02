package net.bicou.redmine.net.upload;

import android.app.Activity;
import android.view.ViewGroup;

import net.bicou.redmine.net.JsonNetworkError;

/**
 * Created by bicou on 08/08/13.
 */
public class JsonUploadError extends JsonNetworkError {
	public enum UploadErrorType {

	}

	UploadErrorType mErrorType;

	public JsonUploadError(UploadErrorType type) {
		mErrorType = type;
	}

	public JsonUploadError(UploadErrorType type, Exception e) {
		super(e);
		mErrorType = type;
	}

	@Override
	public void displayCrouton(final Activity activity, final ViewGroup viewGroup) {
		switch (mErrorType) {
		//TODO
		}
	}

	@Override
	public void displayCrouton(final Activity activity, final int viewGroup) {
		switch (mErrorType) {
		//TODO
		}
	}
}
