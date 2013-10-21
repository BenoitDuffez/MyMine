package net.bicou.redmine.app.wiki;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.gson.Gson;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ProjectsSpinnerAdapter;
import net.bicou.redmine.app.RefreshProjectsTask;
import net.bicou.redmine.app.misc.EmptyFragment;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.util.L;
import net.bicou.splitactivity.SplitActivity;

import java.util.ArrayList;
import java.util.List;

public class WikiActivity extends SplitActivity<WikiPagesListFragment, WikiPageFragment> implements ActionBar.OnNavigationListener,
		AsyncTaskFragment.TaskFragmentCallbacks {
	private WikiPage mDesiredWikiPage;

	@Override
	protected WikiPagesListFragment createMainFragment(final Bundle args) {
		return WikiPagesListFragment.newInstance(args);
	}

	@Override
	protected WikiPageFragment createContentFragment(final Bundle args) {
		return WikiPageFragment.newInstance(args);
	}

	@Override
	protected Fragment createEmptyFragment(final Bundle args) {
		return EmptyFragment.newInstance(R.drawable.empty_wiki);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
		initProjectsSpinner(savedInstanceState);
		AsyncTaskFragment.attachAsyncTaskFragment(this);
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
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
		if (args != null) {
			if (args.containsKey(Constants.KEY_PROJECT_POSITION)) {
				mCurrentProjectPosition = args.getInt(Constants.KEY_PROJECT_POSITION);
			} else if (args.containsKey(WikiPageFragment.KEY_WIKI_PAGE)) {
				mDesiredWikiPage = new Gson().fromJson(args.getString(WikiPageFragment.KEY_WIKI_PAGE), WikiPage.class);
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		initProjectsSpinner(savedInstanceState);
		enableListNavigationMode();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(KEY_REDMINE_PROJECTS_LIST, mProjects);
		outState.putInt(Constants.KEY_PROJECT_POSITION, mCurrentProjectPosition);
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
		if (mProjects != null && mProjects.size() > 0) {
			return;
		}

		new RefreshProjectsTask(WikiActivity.this, new RefreshProjectsTask.ProjectsLoadCallbacks() {
			@Override
			public void onProjectsLoaded(List<Project> projectList) {
				if (mProjects == null) {
					mProjects = new ArrayList<Project>();
				}
				mProjects.addAll(projectList);
				if (mDesiredWikiPage != null) {
					int pos = 0;
					for (Project project : mProjects) {
						if (project.equals(mDesiredWikiPage.project)) {
							mCurrentProjectPosition = pos;
							selectContent(getIntent().getExtras());
							break;
						}
						pos++;
					}
				}
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
		Project currentProject = mProjects.get(mCurrentProjectPosition);

		WikiPagesListFragment listFragment = getMainFragment();
		if (listFragment == null) {
			Bundle args = new Bundle();
			args.putParcelable(WikiPagesListFragment.KEY_PROJECT, currentProject);
			showMainFragment(args);
		} else {
			listFragment.refreshList(currentProject);
		}

		return true;
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		if (action == WikiPageFragment.ACTION_LOAD_WIKI_PAGE) {
			return WikiPageFragment.loadWikiPage(applicationContext, (WikiPageFragment.WikiPageLoadParameters) parameters);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		if (action == WikiPageFragment.ACTION_LOAD_WIKI_PAGE) {
			WikiPageFragment frag = getContentFragment();
			if (frag != null) {
				frag.refreshUI((WikiPageFragment.WikiPageLoadParameters) parameters);
			}
		}
	}
}
