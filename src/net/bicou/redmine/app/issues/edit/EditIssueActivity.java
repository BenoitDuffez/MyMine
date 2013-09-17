package net.bicou.redmine.app.issues.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.net.upload.IssueSerializer;
import net.bicou.redmine.net.upload.JsonUploader;
import net.bicou.redmine.net.upload.ObjectSerializer;
import net.bicou.redmine.util.L;

import java.util.Calendar;

/**
 * Created by bicou on 02/08/13.
 */
public class EditIssueActivity extends SherlockFragmentActivity implements AsyncTaskFragment.TaskFragmentCallbacks, UserPickerDialog.OnUserSelectedListener,
		DatePickerFragment.DateSelectionListener, DescriptionEditorFragment.DescriptionChangeListener {
	public static final int ACTION_LOAD_ISSUE_DATA = 0;
	public static final int ACTION_UPLOAD_ISSUE = 1;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
		super.onCreate(savedInstanceState);
		AsyncTaskFragment.attachAsyncTaskFragment(this);
		if (savedInstanceState == null) {
			Fragment content = EditIssueFragment.newInstance(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, content).commit();
		}
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		if (action == ACTION_LOAD_ISSUE_DATA) {
			return EditIssueFragment.loadSpinnersData(applicationContext, (Issue) parameters);
		} else if (action == ACTION_UPLOAD_ISSUE) {
			Object[] params = (Object[]) parameters;
			Issue issue = (Issue) params[0];
			IssueSerializer issueSerializer = new IssueSerializer(applicationContext, issue, (String) params[1]);
			final String uri;
			if (issue.id <= 0 || issueSerializer.getRemoteOperation() == ObjectSerializer.RemoteOperation.ADD) {
				uri = "issues.json";
			} else {
				uri = "issues/" + issue.id + ".json";
			}
			return new JsonUploader().uploadObject(applicationContext, issue.server, uri, issueSerializer);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		if (action == ACTION_LOAD_ISSUE_DATA) {
			EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
			frag.setupSpinners((EditIssueFragment.IssueEditInformation) result);
		} else if (action == ACTION_UPLOAD_ISSUE) {
			L.d("Upload issue json: " + result);
			finish();
		}
	}

	@Override
	public void onUserSelected(final User user) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		frag.onAssigneeChosen(user);
	}

	@Override
	public void onDateSelected(final int id, final Calendar calendar) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		frag.onDatePicked(id, calendar);
	}

	@Override
	public void onDescriptionChanged(final String newDescription) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		frag.onDescriptionChanged(newDescription);
	}
}
