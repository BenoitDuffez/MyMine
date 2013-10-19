package net.bicou.redmine.app.projects;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.analytics.tracking.android.EasyTracker;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.misc.EmptyFragment;
import net.bicou.redmine.app.welcome.OverviewCard;
import net.bicou.redmine.data.json.Project;
import net.bicou.splitactivity.SplitActivity;

import java.util.List;

public class ProjectsActivity extends SplitActivity<ProjectsListFragment, ProjectFragment> implements AsyncTaskFragment.TaskFragmentCallbacks {
	public static final int ACTION_LOAD_PROJECT_CARDS = 0;

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
	protected void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
		super.onCreate(savedInstanceState);
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
		switch (action) {
		case ACTION_LOAD_PROJECT_CARDS:
			Project project = (Project) parameters;
			return ProjectFragment.getProjectCards(applicationContext, project.server, project);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		ProjectFragment projectFragment = getContentFragment();
		if (projectFragment != null) {
			projectFragment.onCardsBuilt((List<OverviewCard>) result);
		}
	}
}
