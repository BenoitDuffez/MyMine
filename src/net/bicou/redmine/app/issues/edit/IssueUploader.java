package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.gson.Gson;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssueFragment;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.net.upload.IssueSerializer;
import net.bicou.redmine.net.upload.JsonUploader;
import net.bicou.redmine.net.upload.ObjectSerializer;
import net.bicou.redmine.util.L;

/**
 * Helper class to handle issue uploads
 * Created by bicou on 26/09/13.
 */
public class IssueUploader {
	// Constants for the startActivityForResult:
	public static final int CREATE_ISSUE = 0;
	public static final int EDIT_ISSUE = 1;
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
		if (result instanceof JsonNetworkError) {
			Crouton.makeText(resultHolder, String.format(resultHolder.getString(R.string.issue_upload_failed), ((JsonNetworkError) result).getMessage(resultHolder)
			), Style.ALERT).show();
		} else {
			if (params != null && params.containsKey(ISSUE_ACTION) && params.getInt(ISSUE_ACTION) == CREATE_ISSUE) {
				Issue issue = new Gson().fromJson(params.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
				Bundle args = new Bundle();
				args.putLong(Constants.KEY_ISSUE_ID, issue.id);
				args.putLong(Constants.KEY_ISSUE_ID, issue.server.rowId);
				args.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(issue, Issue.class));
				if (resultHolder instanceof IssuesActivity) {
					((IssuesActivity) resultHolder).selectContent(args);
				} else {
					Intent intent = new Intent(resultHolder, IssuesActivity.class);
					intent.putExtras(args);
					resultHolder.startActivity(intent);
				}
			} else {
				Crouton.makeText(resultHolder, resultHolder.getString(R.string.issue_upload_successful), Style.CONFIRM).show();
				L.e("Shouldn't happen! params=" + params, null);
			}
		}
		L.d("Upload issue json: " + result);
	}
}
