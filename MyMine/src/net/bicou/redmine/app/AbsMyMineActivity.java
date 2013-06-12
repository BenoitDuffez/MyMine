package net.bicou.redmine.app;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.util.L;

import java.util.ArrayList;

public abstract class AbsMyMineActivity extends SherlockFragmentActivity implements ActionBar.OnNavigationListener {
	/**
	 * Must return true if the projects spinner in the action bar is required for this activity
	 *
	 * @return whether the activity should load the projects list
	 */
	abstract protected boolean shouldDisplayProjectsSpinner();

	/**
	 * Called when the projects spinner from the action bar has been modified by the user
	 */
	abstract protected void onCurrentProjectChanged();

	public interface SplitScreenFragmentConfigurationChangesListener {
		public void updateSplitScreenState(final boolean isSplitScreen);

		public void updateCurrentProject(long projectId);
	}

	protected boolean mIsSplitScreen;

	public boolean isSplitScreen() {
		return mIsSplitScreen;
	}

	public interface SplitScreenBehavior {
		/**
		 * Whether the screen is split into a list + an item. Likely the case on tablets and/or in landscape orientation
		 */
		public static final String KEY_IS_SPLIT_SCREEN = "net.bicou.redmine.issues.SplitScreen";

	}


	public static final String KEY_REDMINE_PROJECTS_LIST = "net.bicou.mymine.RedmineProjectsList";

	protected ArrayList<Project> mProjects;
	protected ArrayAdapter<Project> mAdapter;
	public int mCurrentProjectPosition;

	protected void onPreCreate() {
		// empty, can be overriden by subclasses
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		onPreCreate();

		mCurrentProjectPosition = -1;
		if (savedInstanceState == null) {
			mProjects = new ArrayList<Project>();
			mAdapter = new ProjectsSpinnerAdapter(this, R.layout.main_nav_item, mProjects);
		}

		// Don't duplicate fragments if they're already created
		if (savedInstanceState == null) {
			// Show up button
			//TODO: handle proper use of the up button
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		// Specific project/server?
		final Bundle args = getIntent().getExtras();
		if (args != null && args.containsKey(Constants.KEY_PROJECT_POSITION)) {
			mCurrentProjectPosition = args.getInt(Constants.KEY_PROJECT_POSITION);
		}
	}

	protected void prepareIndeterminateProgressActionBar() {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			//TODO
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(KEY_REDMINE_PROJECTS_LIST, mProjects);
		outState.putInt(Constants.KEY_PROJECT_POSITION, mCurrentProjectPosition);
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

			if (shouldDisplayProjectsSpinner()) {
				enableListNavigationMode();
			}
		}
	}


	public Project getCurrentProject() {
		if (mProjects != null && mCurrentProjectPosition >= 0 && mCurrentProjectPosition < mProjects.size()) {
			return mProjects.get(mCurrentProjectPosition);
		}
		return null;
	}

	public Server getCurrentServer() {
		final Project p = getCurrentProject();
		if (p != null) {
			return p.server;
		}
		return null;
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

	private class RefreshProjectsTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(final Void... params) {
			L.d("");
			final ProjectsDbAdapter db = new ProjectsDbAdapter(AbsMyMineActivity.this);
			db.open();
			mProjects.clear();
			mProjects.addAll(db.selectAll());
			db.close();

			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			L.d("");

			if (mProjects.size() > 0) {
				if (getSupportActionBar().getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
					enableListNavigationMode();
				}
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	public void refreshProjectsList() {
		if (mProjects.size() > 0 || shouldDisplayProjectsSpinner() == false) {
			return;
		}

		new RefreshProjectsTask().execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		mProjects.clear();
		refreshProjectsList();
	}

	@Override
	public boolean onNavigationItemSelected(final int itemPosition, final long itemId) {
		L.d("position: " + itemPosition);
		if (mProjects == null || itemPosition < 0 || itemPosition > mProjects.size()) {
			return true;
		}

		mCurrentProjectPosition = itemPosition;
		onCurrentProjectChanged();

		return true;
	}

	private class ProjectsSpinnerAdapter extends ArrayAdapter<Project> {
		ArrayList<Project> data;
		LayoutInflater inflater;

		public ProjectsSpinnerAdapter(final Context ctx, final int textViewResourceId, final ArrayList<Project> data) {
			super(ctx, textViewResourceId, data);
			this.data = data;
			inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.main_nav_item_in_actionbar, null);
			}

			final TextView text = (TextView) convertView.findViewById(R.id.main_nav_ab_item_text);

			if (text != null && data != null && position < data.size()) {
				text.setText(data.get(position).name);
			}

			return convertView;
		}

		@Override
		public View getDropDownView(final int position, View convertView, final ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.main_nav_item, null);
			}

			final TextView text = (TextView) convertView.findViewById(R.id.main_nav_item_text);
			final View image = convertView.findViewById(R.id.main_nav_item_icon);

			if (text != null && image != null) {
				text.setText(data.get(position).name);
				image.setVisibility(View.INVISIBLE);
			}

			return convertView;
		}

		@Override
		public int getCount() {
			return data == null ? 0 : data.size();
		}

		@Override
		public Project getItem(final int position) {
			return null;
		}

		@Override
		public long getItemId(final int position) {
			return 0;
		}
	}
}
