package net.bicou.redmine.app.projects;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.issues.edit.IssueUploader;
import net.bicou.redmine.app.misc.EmptyFragment;
import net.bicou.redmine.app.welcome.OverviewCard;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.util.L;
import net.bicou.splitactivity.SplitActivity;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ProjectsActivity extends SplitActivity<ProjectsListFragment, ProjectFragment> implements AsyncTaskFragment.TaskFragmentCallbacks {
	public static final int ACTION_LOAD_PROJECT_CARDS = 0;
	public static final int ACTION_UPLOAD_ISSUE = 1;

	@Override
	protected ProjectsListFragment createMainFragment(Bundle args) {
		return ProjectsListFragment.newInstance(args);
	}

	@Override
	protected ProjectFragment createContentFragment(Bundle args) {
		return ProjectFragment.newInstance(args);
	}

	@Override
	protected Fragment createEmptyFragment(Bundle args) {
		return EmptyFragment.newInstance(R.drawable.projects_empty_fragment);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPreCreate() {
		supportRequestWindowFeature(Window.FEATURE_PROGRESS);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
		AsyncTaskFragment.attachAsyncTaskFragment(this);

		// Load a project if there is one in the intent extras
		if (savedInstanceState == null) {
			Bundle args = getIntent().getExtras();
			if (args != null && args.keySet().contains(ProjectFragment.KEY_PROJECT_JSON)) {
				selectContent(args);
			}
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		L.d("requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
		if (resultCode == RESULT_OK) {
			final Bundle extras = data == null || data.getExtras() == null ? new Bundle() : data.getExtras();
			extras.putInt(IssueUploader.ISSUE_ACTION, requestCode);
			AsyncTaskFragment.runTask(this, ACTION_UPLOAD_ISSUE, extras);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().getBoolean(IssueUploader.KEY_SHOW_ISSUE_UPLOAD_SUCCESSFUL_CROUTON)) {
			Crouton.makeText(this, getString(R.string.issue_upload_successful), Style.CONFIRM).show();
		}
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
		switch (action) {
		case ACTION_LOAD_PROJECT_CARDS:
			Project project = (Project) parameters;
			return ProjectFragment.getProjectCards(applicationContext, project.server, project);

		case ACTION_UPLOAD_ISSUE:
			return IssueUploader.uploadIssue(applicationContext, (Bundle) parameters);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		switch (action) {
		case ACTION_LOAD_PROJECT_CARDS:
			ProjectFragment projectFragment = getContentFragment();
			if (projectFragment != null) {
				projectFragment.onCardsBuilt((List<OverviewCard>) result);
			}
			break;

		case ACTION_UPLOAD_ISSUE:
			IssueUploader.handleAddEdit(this, (Bundle) parameters, result);
			break;
		}
	}
}
