package net.bicou.redmine.app.wiki;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import com.actionbarsherlock.app.ActionBar;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ProjectsSpinnerAdapter;
import net.bicou.redmine.app.RefreshProjectsTask;
import net.bicou.redmine.app.drawers.DrawerActivity;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.List;

public class WikiActivity extends DrawerActivity implements ActionBar.OnNavigationListener {
	private static final String WIKI_CONTENTS_TAG = "wiki";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initProjectsSpinner(savedInstanceState);

		// Setup fragments
		if (savedInstanceState == null) {
			final Bundle args = new Bundle(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().replace(R.id.content, WikiPageFragment.newInstance(args), WIKI_CONTENTS_TAG).commit();
		}
	}

	//-----------------------------------------------------------
	// Projects spinner


	@Override
	protected void onResume() {
		super.onResume();
		refreshProjectsList();
	}

	public static final String KEY_REDMINE_PROJECTS_LIST = "net.bicou.mymine.RedmineProjectsList";

	protected ArrayList<Project> mProjects;
	protected ArrayAdapter<Project> mAdapter;
	public int mCurrentProjectPosition;

	private void initProjectsSpinner(Bundle savedInstanceState) {
		mCurrentProjectPosition = -1;
		if (savedInstanceState == null) {
			mProjects = new ArrayList<Project>();
			mAdapter = new ProjectsSpinnerAdapter(this, R.layout.main_nav_item, mProjects);
		}

		// Specific project/server?
		final Bundle args = getIntent().getExtras();
		if (args != null && args.containsKey(Constants.KEY_PROJECT_POSITION)) {
			mCurrentProjectPosition = args.getInt(Constants.KEY_PROJECT_POSITION);
		}
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Prepare navigation spinner
		if (savedInstanceState == null) {
			mProjects = new ArrayList<Project>();
			mAdapter = new ProjectsSpinnerAdapter(this, R.layout.main_nav_item, mProjects);
			mCurrentProjectPosition = -1;
		} else {
			mProjects = savedInstanceState.getParcelableArrayList(KEY_REDMINE_PROJECTS_LIST);
			mAdapter = new ProjectsSpinnerAdapter(this, R.layout.main_nav_item, mProjects);
			mCurrentProjectPosition = savedInstanceState.getInt(Constants.KEY_PROJECT_POSITION);

			enableListNavigationMode();
		}
	}

	private void enableListNavigationMode() {
		L.d("current proj pos=" + mCurrentProjectPosition);
		final ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ab.setListNavigationCallbacks(mAdapter, this);
		if (mCurrentProjectPosition >= 0) {
			ab.setSelectedNavigationItem(mCurrentProjectPosition);
		}
	}

	public void refreshProjectsList() {
		if (mProjects.size() > 0) {
			return;
		}

		new RefreshProjectsTask(WikiActivity.this, new RefreshProjectsTask.ProjectsLoadCallbacks() {
			@Override
			public void onProjectsLoaded(List<Project> projectList) {
				mProjects.addAll(projectList);
				if (getSupportActionBar().getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
					enableListNavigationMode();
				}
			}
		}).execute();
	}

	@Override
	public boolean onNavigationItemSelected(final int itemPosition, final long itemId) {
		L.d("position: " + itemPosition);
		if (mProjects == null || itemPosition < 0 || itemPosition > mProjects.size()) {
			return true;
		}

		mCurrentProjectPosition = itemPosition;

		final WikiPageFragment f = (WikiPageFragment) getSupportFragmentManager().findFragmentById(R.id.content);
		f.updateCurrentProject(mProjects.get(mCurrentProjectPosition));

		return true;
	}
}
