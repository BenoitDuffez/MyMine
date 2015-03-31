package net.bicou.redmine.app.settings;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.SimpleCursorLoader;
import net.bicou.redmine.data.sqlite.TrackersDbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.sync.IssuesSyncAdapterService;
import net.bicou.redmine.sync.ProjectsSyncAdapterService;
import net.bicou.redmine.sync.WikiSyncAdapterService;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Lists all projects and allow them to be marked as not syncable
 * Created by bicou on 04/03/2014.
 */
public class ProjectSyncSettingsActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>, AsyncTaskFragment.TaskFragmentCallbacks {
	ListView mListView;
	ProjectsDbAdapter mDb;
	private static final String[] FROM_COLUMNS = {
			ProjectsDbAdapter.KEY_NAME,
			ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED,
			ProjectsDbAdapter.KEY_SERVER_ID,
	};
	private static final int[] TO_VIEWS = {
			android.R.id.text1,
			android.R.id.text1,
			android.R.id.text1,
	};
	private static final String[] SELECTION_COLUMNS = {
			ProjectsDbAdapter.KEY_ID + " AS " + DbAdapter.KEY_ROWID,
			ProjectsDbAdapter.KEY_ID,
			ProjectsDbAdapter.KEY_NAME,
			ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED,
			ProjectsDbAdapter.KEY_SERVER_ID,
	};
	SimpleCursorAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_PROGRESS);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_project_sync_settings);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
		mListView = (ListView) findViewById(android.R.id.list);
		getSupportLoaderManager().restartLoader(1, null, this);
		mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_checked, null, FROM_COLUMNS, TO_VIEWS, 0);
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			                       @Override
			                       public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				                       CheckedTextView ctv = (CheckedTextView) view;
				                       String s = cursor.getColumnName(columnIndex);
				                       if (ProjectsDbAdapter.KEY_NAME.equals(s)) {
					                       ctv.setText(cursor.getString(columnIndex));
					                       return true;
				                       } else if (ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED.equals(s)) {
					                       ctv.setChecked(cursor.getInt(columnIndex) > 0);
					                       return true;
				                       } else if (ProjectsDbAdapter.KEY_SERVER_ID.equals(s)) {
					                       ctv.setTag(cursor.getLong(columnIndex));
					                       return true;
				                       } else {
					                       return false;
				                       }
			                       }
		                       }
		);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			                                 @Override
			                                 public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				                                 final CheckedTextView checkedTextView = (CheckedTextView) view;
				                                 checkedTextView.toggle();

				                                 long projectId = parent.getAdapter().getItemId(position);
				                                 final long serverId = (Long) view.getTag();
				                                 Project project = mDb.select(serverId, projectId, null);
				                                 project.is_sync_blocked = checkedTextView.isChecked();
				                                 mDb.update(project);
			                                 }
		                                 }
		);
		mListView.setAdapter(mAdapter);

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
	protected void onPause() {
		super.onPause();

		if (mDb != null) {
			mDb.close();
			mDb = null;
		}
	}

	@Override
	public void onBackPressed() {
		//super.onBackPressed(); // I know, it's bad.

		if (AsyncTaskFragment.isRunning(0)) {
			displayCrouton();
		} else {
			AsyncTaskFragment.runTask(this, 0, getHelper());
		}
	}

	@Override
	public void onPreExecute(int action, Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
		displayCrouton();
	}

	private void displayCrouton() {
		Crouton.makeText(this, getString(R.string.projects_sync_saving), Style.INFO).show();
	}

	@Override
	public Object doInBackGround(Context applicationContext, int action, Object parameters) {
		ProjectsDbAdapter db = (ProjectsDbAdapter) parameters;

		// Remove data from newly checked projects
		List<Project> projects = db.getBlockedProjects();
		if (projects != null && projects.size() > 0) {
			IssuesDbAdapter issuesDb = new IssuesDbAdapter(db);
			WikiDbAdapter wikiDb = new WikiDbAdapter(db);
			VersionsDbAdapter versionsDb = new VersionsDbAdapter(db);
			IssueCategoriesDbAdapter issueCategoriesDb = new IssueCategoriesDbAdapter(db);
			TrackersDbAdapter trackersDb = new TrackersDbAdapter(db);
			for (Project project : projects) {
				issuesDb.deleteAll(project.server, project.id);
				wikiDb.deleteAll(project.server, project.id);
				versionsDb.deleteAll(project.server, project.id);
				issueCategoriesDb.deleteAll(project.server, project.id);
				trackersDb.deleteAll(project.server, project.id);
			}
		}

		// Trigger a new sync in order to re-download newly unchecked projects
		ServersDbAdapter serversDb = new ServersDbAdapter(db);
		List<Server> servers = serversDb.selectAll();
		IssuesSyncAdapterService.Synchronizer issuesSync = new IssuesSyncAdapterService.Synchronizer(this);
		WikiSyncAdapterService.Synchronizer wikiSync = new WikiSyncAdapterService.Synchronizer(this);
		ProjectsSyncAdapterService.Synchronizer projectsSync = new ProjectsSyncAdapterService.Synchronizer(this);
		for (Server server : servers) {
			issuesSync.synchronizeIssues(server, null, 0);
			wikiSync.synchronizeWikiPages(server, 0);
			projectsSync.synchronizeProjects(server, 0);
		}

		return null;
	}

	@Override
	public void onPostExecute(int action, Object parameters, Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		finish();
	}

	private ProjectsDbAdapter getHelper() {
		if (mDb == null) {
			mDb = new ProjectsDbAdapter(this);
			mDb.open();
		}
		return mDb;
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final ProjectsDbAdapter helper = getHelper();
		return new SimpleCursorLoader(this) {
			@Override
			public Cursor loadInBackground() {
				return helper.selectAllCursor(0, SELECTION_COLUMNS, null);
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
