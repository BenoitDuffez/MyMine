package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.Gson;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.issues.issue.IssueFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.ErrorsList;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.net.JsonDownloadError;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.net.upload.IssueSerializer;
import net.bicou.redmine.net.upload.JsonUploader;
import net.bicou.redmine.net.upload.ObjectSerializer;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import org.apache.http.HttpStatus;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Helper class to handle issue uploads
 * Created by bicou on 26/09/13.
 */
public class IssueUploader {
	// Constants for the startActivityForResult:
	public static final int CREATE_ISSUE = 0;
	public static final int EDIT_ISSUE = 1;

	/**
	 * Used to store the action (see above) in a Bundle so that we can retrieve later on what was the required action
	 */
	public static final String ISSUE_ACTION = "net.bicou.redmine.app.issues.IssueAction";

	/**
	 * Used as a key in a Bundle to indicate whether the activity should display a Crouton if its intent extras contains this as true
	 */
	public static final String KEY_SHOW_ISSUE_UPLOAD_SUCCESSFUL_CROUTON = "net.bicou.redmine.app.issues.IssueUploadSuccessful";

	/**
	 * If set in a Bundle, the activity that receives it should display an error Crouton which message is contained in the bundle at that key
	 */
	public static final String KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON = "net.bicou.redmine.app.issues.IssueUploadSuccessful";

	/**
	 * Add or edit an issue
	 *
	 * @param applicationContext Required for network operations (SSL) and DB access
	 * @param params             Bundle that contains the issue as a JSON string, the issue modification notes if it's an edit, and the issue action (see {@link
	 *                           #ISSUE_ACTION}
	 *
	 * @return The server's response, or a {@link net.bicou.redmine.net.JsonNetworkError}
	 */
	public static Object uploadIssue(final Context applicationContext, final Bundle params) {
		String uri;
		Issue issue = new Gson().fromJson(params.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
		final String notes = params.getString(EditIssueFragment.KEY_ISSUE_NOTES);
		IssueSerializer issueSerializer = new IssueSerializer(applicationContext, issue, notes);
		issueSerializer.build();
		if (issue.id <= 0 || issueSerializer.getRemoteOperation() == ObjectSerializer.RemoteOperation.ADD) {
			uri = "issues.json";
		} else {
			uri = "issues/" + issue.id + ".json";
		}
		return new JsonUploader().uploadObject(applicationContext, issue.server, uri, issueSerializer);
	}

	/**
	 * Handle the result of an add/edit issue
	 *
	 * @param resultHolder The activity that will receive the UI notification to the user
	 * @param params       The task launching params
	 * @param result       The server's response
	 */
	public static void handleAddEdit(Activity resultHolder, Bundle params, Object result) {
		L.d("Upload issue json: " + result);

		int action = params.getInt(ISSUE_ACTION);
		final Bundle args = doHandleAddEdit(resultHolder, params, result);

		// Don't fail this, it may be because the Activity has been closed while we where uploading. Doesn't matter.
		try {
			// Do nothing, we expect the caller to handle an empty response by itself
			if (args == null) {
				L.i("The server returned an empty response, while we expected to have issue details. Nevermind.");
			}
			// Re-open the edit issue activity
			else if (args.containsKey(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON)) {
				Intent intent = new Intent(resultHolder, EditIssueActivity.class);
				intent.putExtra(IssueFragment.KEY_ISSUE_JSON, params.getString(IssueFragment.KEY_ISSUE_JSON));
				intent.putExtra(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON, args.getString(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON));
				resultHolder.startActivityForResult(intent, action);
			}
			// If we're already on the issues activity, let it handle the new issue to display
			else if (resultHolder instanceof IssuesActivity) {
				((IssuesActivity) resultHolder).selectContent(args);
				final String croutonText = resultHolder.getString(action == CREATE_ISSUE ? R.string.issue_upload_successful : R.string.issue_update_successful);
				Crouton.makeText(resultHolder, croutonText, Style.CONFIRM).show();
			}
			// Otherwise, start it from scratch
			else {
				Intent intent = new Intent(resultHolder, IssuesActivity.class);
				intent.putExtras(args);
				intent.putExtra(KEY_SHOW_ISSUE_UPLOAD_SUCCESSFUL_CROUTON, true);
				resultHolder.startActivityForResult(intent, action);
			}
		} catch (Exception e) {
			L.e("This failed, but it shouldn't have. Maybe not a problem though.", e);
		}
	}

	/**
	 * Handle the result and provide feedback
	 *
	 * @param resultHolder The activity that will receive the UI notification to the user
	 * @param params       The task launching params
	 * @param result       The server's response
	 *
	 * @return A bundle used to update the UI after this operation
	 */
	private static Bundle doHandleAddEdit(Activity resultHolder, Bundle params, Object result) {
		Bundle args = new Bundle();

		// Network error?
		if (result instanceof JsonNetworkError) {
			final JsonNetworkError networkError = (JsonNetworkError) result;
			final String errorMessage;

			// Prepare error message
			String msg = networkError.getMessage(resultHolder);

			// The server understood but returned an error
			if (networkError.httpResponseCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
				ErrorsList errorsList = new Gson().fromJson(networkError.json, ErrorsList.class);
				if (errorsList != null && errorsList.errors != null && errorsList.errors.size() > 0) {
					String[] errors = errorsList.errors.toArray(new String[errorsList.errors.size()]);
					errorMessage = String.format(resultHolder.getString(R.string.issue_upload_failed), Util.join(errors, ", "));
				} else {
					if (TextUtils.isEmpty(msg)) {
						msg = networkError.json;
					} else {
						msg += " (" + networkError.json + ")";
					}
					final int resId = msg == null ? R.string.issue_upload_failed_nodetails : R.string.issue_upload_failed;
					errorMessage = String.format(resultHolder.getString(resId), msg);
				}
			}
			// The server didn't reply or we didn't understand each other
			else {
				final int resId = TextUtils.isEmpty(msg) ? R.string.issue_upload_failed_nodetails : R.string.issue_upload_failed;
				errorMessage = String.format(resultHolder.getString(resId), networkError.getMessage(resultHolder));
			}

			args.putString(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON, errorMessage);
			return args;
		}
		// Params contains the data necessary to open the issues activity (notably, the server ID)
		else if (params != null) {
			// Retrieve server (used to display the issue later on)
			Issue uploadedIssue = new Gson().fromJson(params.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
			if (uploadedIssue == null) {
				return null;
			}
			Server server = uploadedIssue.server;

			// Retrieve the issue as understood by the server
			String json = (String) result;
			Issue issue;

			try {
				// Parse the server's response
				final int start = json.indexOf(":") + 1;
				final int end = json.lastIndexOf("}");
				json = json.substring(start, end);

				Object response = parseJson(json);
				if (response == null || response instanceof JsonDownloadError) {
					final String msg = response == null ? resultHolder.getString(R.string.err_empty_response) : ((JsonDownloadError) response).getMessage(resultHolder);
					final int resId = TextUtils.isEmpty(msg) ? R.string.issue_upload_failed_nodetails : R.string.issue_upload_failed;
					final String errorMessage = resultHolder.getString(resId, msg);
					args.putString(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON, errorMessage);
					L.e("Shouldn't happen! params=" + params + " result=" + result, null);
					return args;
				}

				issue = (Issue) response;
				final TimeZone timeZone = new GregorianCalendar().getTimeZone();
				issue.created_on.setTimeZone(timeZone);
				issue.updated_on.setTimeZone(timeZone);
				issue.due_date.setTimeZone(timeZone);
				issue.start_date.setTimeZone(timeZone);
				issue.server = server;
			} catch (Exception e) {
				// In case of failure:
				switch (params.getInt(ISSUE_ACTION)) {
				// if it's a creation, it's likely to be an error message, so we'll display it
				case CREATE_ISSUE:
					String errorMessage = resultHolder.getString(R.string.issue_upload_failed, (String) result);
					args.putString(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON, errorMessage);
					L.e("Unable to upload issue. Result=" + result + " params=" + params, null);
					return args;

				// if it's an edit, it's likely to be an empty response, so we'll consider our edit to be successful
				case EDIT_ISSUE:
					issue = uploadedIssue;
					L.e("The server's response wasn't expected!" + e);
					break;

				default:
					throw new IllegalArgumentException("Unhandled case constant!");
				}
			}

			// Handle add/edit of issue
			if (params.containsKey(ISSUE_ACTION)) {
				IssuesDbAdapter db = new IssuesDbAdapter(resultHolder);
				db.open();
				switch (params.getInt(ISSUE_ACTION)) {
				case CREATE_ISSUE:
					db.insert(issue);
					break;
				case EDIT_ISSUE:
					db.update(issue);
					break;
				}
				db.close();
			}

			// Prepare args to open issues activity showing that issue
			args.putLong(Constants.KEY_ISSUE_ID, issue.id);
			args.putLong(Constants.KEY_SERVER_ID, issue.server.rowId);
			args.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(issue, Issue.class));

			return args;
		}
		// No params, but yet successful? Doesn't matter, display a nice crouton
		else {
			final String errorMessage = resultHolder.getString(R.string.issue_upload_successful);
			args.putString(KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON, errorMessage);
			L.e("Shouldn't happen! params are null", null);
			return args;
		}
	}

	/**
	 * Use the {@link net.bicou.redmine.net.JsonDownloader#gsonParse(String, Class)} Gson wrapper to get an issue out of the downloaded JSON String
	 *
	 * @param json The JSON string
	 *
	 * @return An {@link net.bicou.redmine.data.json.Issue} if the JSON could be parsed, a {@link net.bicou.redmine.net.JsonNetworkError} if the JSON could not
	 * be downloaded/parsed/etc.
	 */
	private static Object parseJson(String json) {
		Issue object = JsonDownloader.gsonParse(json, Issue.class);

		if (object == null) {
			JsonDownloadError error = new JsonDownloadError(JsonDownloadError.ErrorType.TYPE_JSON);
			error.setMessage(R.string.err_parse_error);
			return error;
		}

		return object;
	}

	/**
	 * Delete an issue
	 *
	 * @param applicationContext Required for network operations (SSL) and DB access
	 * @param issue              The issue to be deleted
	 *
	 * @return The server's response, or a {@link net.bicou.redmine.net.JsonNetworkError}
	 */
	public static Object deleteIssue(final Context applicationContext, final Issue issue) {
		IssueSerializer serializer = new IssueSerializer(applicationContext, issue, null, true);
		serializer.build();
		String uri = "issues/" + issue.id + ".json";
		return new JsonUploader().uploadObject(applicationContext, issue.server, uri, serializer);
	}

	/**
	 * Handle the result of a delete issue
	 *
	 * @param resultHolder The activity that will receive the UI notification to the user
	 * @param issue        The issue that was meant to be deleted
	 * @param result       The server's response
	 */
	public static void handleDelete(Activity resultHolder, Issue issue, Object result) {
		L.d("delete issue: " + result);
		if (result == null || !(result instanceof JsonNetworkError)) {
			IssuesDbAdapter db = new IssuesDbAdapter(resultHolder);
			db.open();
			db.delete(issue);
			db.close();
			Crouton.makeText(resultHolder, resultHolder.getString(R.string.issue_delete_confirmed), Style.CONFIRM).show();
		} else {
			((JsonNetworkError) result).displayCrouton(resultHolder, null);
		}
	}
}
