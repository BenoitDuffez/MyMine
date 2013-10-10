package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssueFragment;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.CalendarDeserializer;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.json.VersionStatusDeserializer;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.net.JsonDownloadError;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.net.upload.IssueSerializer;
import net.bicou.redmine.net.upload.JsonUploader;
import net.bicou.redmine.net.upload.ObjectSerializer;
import net.bicou.redmine.util.L;

import java.util.Calendar;

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

	public static Object uploadIssue(final Context applicationContext, final Bundle params) {
		String uri;
		Issue issue = new Gson().fromJson(params.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
		IssueSerializer issueSerializer = new IssueSerializer(applicationContext, issue, params.getString(EditIssueFragment.KEY_ISSUE_NOTES));
		if (issue.id <= 0 || issueSerializer.getRemoteOperation() == ObjectSerializer.RemoteOperation.ADD) {
			uri = "issues.json";
		} else {
			uri = "issues/" + issue.id + ".json";
		}
		return new JsonUploader().uploadObject(applicationContext, issue.server, uri, issueSerializer);
	}

	public static void handleResult(Activity resultHolder, Bundle params, Object result) {
		L.d("Upload issue json: " + result);

		// Network error?
		if (result instanceof JsonNetworkError) {
			final String errorMessage = String.format(resultHolder.getString(R.string.issue_upload_failed), ((JsonNetworkError) result).getMessage(resultHolder));
			Crouton.makeText(resultHolder, errorMessage, Style.ALERT).show();
		}
		// Response error?
		else if (TextUtils.isEmpty((CharSequence) result)) {
			String errorMessage = resultHolder.getString(R.string.issue_upload_failed, (String) result);
			Crouton.makeText(resultHolder, errorMessage, Style.ALERT).show();
			L.e("Unable to upload issue. Result=" + result + " params=" + params, null);
		}
		// Success!
		else {
			// Params contains the data necessary to open the issues activity (notably, the server ID)
			if (params != null) {
				// Retrieve server (used to display the issue later on)
				Issue uploadedIssue = new Gson().fromJson(params.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
				Server server = uploadedIssue.server;

				// Retrieve the issue as understood by the server
				String json = (String) result;

				final int start = json.indexOf(":") + 1;
				final int end = json.lastIndexOf("}");
				json = json.substring(start, end);

				Object response = parseJson(json);
				if (response == null || response instanceof JsonDownloadError) {
					final String msg = response == null ? resultHolder.getString(R.string.err_empty_response) : ((JsonDownloadError) response).getMessage
							(resultHolder);
					final String errorMessage = resultHolder.getString(R.string.issue_upload_failed, msg);
					Crouton.makeText(resultHolder, errorMessage, Style.ALERT).show();
					L.e("Shouldn't happen! params=" + params + " result=" + result, null);
					return;
				}

				Issue issue = (Issue) response;
				issue.server = server;

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
				Bundle args = new Bundle();
				args.putLong(Constants.KEY_ISSUE_ID, issue.id);
				args.putLong(Constants.KEY_SERVER_ID, issue.server.rowId);
				args.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(issue, Issue.class));

				// If we're already on the issues activity, let it handle the new issue to display
				if (resultHolder instanceof IssuesActivity) {
					((IssuesActivity) resultHolder).selectContent(args);
				}
				// Otherwise, start it from scratch
				else {
					Intent intent = new Intent(resultHolder, IssuesActivity.class);
					intent.putExtras(args);
					resultHolder.startActivity(intent);
				}
			}
			// No params, but yet successful? Doesn't matter, display a nice crouton
			else {
				Crouton.makeText(resultHolder, resultHolder.getString(R.string.issue_upload_successful), Style.CONFIRM).show();
				L.e("Shouldn't happen! params=" + params, null);
			}
		}
	}

	/**
	 * TODO: merge this with {@link net.bicou.redmine.net.JsonDownloader#parseJson(String)}
	 */
	private static Object parseJson(String json) {
		Issue object = null;

		try {
			final GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(Calendar.class, new CalendarDeserializer());
			builder.registerTypeAdapter(Version.VersionStatus.class, new VersionStatusDeserializer());
			final Gson gson = builder.create();

			object = gson.fromJson(json, Issue.class);
		} catch (final JsonSyntaxException e) {
			L.e("Unparseable JSON is:");
			L.e(json);
		} catch (final IllegalStateException e) {
			L.e("Unparseable JSON is:");
			L.e(json);
		} catch (final Exception e) {
			L.e("Unparseable JSON is:");
			L.e(json);
			L.e("Unable to parse JSON", e);
		}

		if (object == null) {
			JsonDownloadError error = new JsonDownloadError(JsonDownloadError.ErrorType.TYPE_JSON);
			error.setMessage(R.string.err_parse_error);
			return error;
		}

		return object;
	}
}
