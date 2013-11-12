package net.bicou.redmine.app.issues.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.util.Util;

import java.util.Calendar;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by bicou on 02/08/13.
 */
public class EditIssueActivity extends ActionBarActivity implements AsyncTaskFragment.TaskFragmentCallbacks, UserPickerDialog.OnUserSelectedListener,
		DatePickerFragment.DateSelectionListener, DescriptionEditorFragment.DescriptionChangeListener , ProjectSwitcherFragment.ProjectChangeListener{
	public static final int ACTION_LOAD_ISSUE_DATA = 0;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_PROGRESS);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.fragment_activity);
		AsyncTaskFragment.attachAsyncTaskFragment(this);
		if (savedInstanceState == null) {
			Fragment content = EditIssueFragment.newInstance(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(Util.getContentViewCompat(), content).commit();
		}

		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		if (action == ACTION_LOAD_ISSUE_DATA) {
			return EditIssueFragment.loadSpinnersData(applicationContext, (Issue) parameters);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		if (action == ACTION_LOAD_ISSUE_DATA) {
			EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
			frag.setupSpinners((EditIssueFragment.IssueEditInformation) result);

			if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(IssueUploader.KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON)) {
				Crouton.makeText(this, getIntent().getExtras().getString(IssueUploader.KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON), Style.ALERT).show();
			}
		}
	}

	@Override
	public void onUserSelected(final User user) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onAssigneeChosen(user);
	}

	@Override
	public void onDateSelected(final int id, final Calendar calendar) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onDatePicked(id, calendar);
	}

	@Override
	public void onDescriptionChanged(final String newDescription) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onDescriptionChanged(newDescription);
	}

	@Override
	public void onProjectChanged(long newProjectId) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onProjectChanged(newProjectId);
	}
}
