package net.bicou.redmine.app.settings;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;

import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.SimpleCursorLoader;

/**
 * Lists all projects and allow them to be marked as not syncable
 * Created by bicou on 04/03/2014.
 */
public class ProjectSyncSettingsActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	ListView mListView;
	ProjectsDbAdapter mDb;
	private static final String[] FROM_COLUMNS = { ProjectsDbAdapter.KEY_NAME };
	private static final String[] SELECTION_COLUMNS = { "rowid AS " + DbAdapter.KEY_ROWID, ProjectsDbAdapter.KEY_ID, ProjectsDbAdapter.KEY_NAME, ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED, ProjectsDbAdapter.KEY_SERVER_ID };
	private static final int[] TO_VIEWS = { android.R.id.text1 };
	SimpleCursorAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_project_sync_settings);
		mListView = (ListView) findViewById(android.R.id.list);
		getSupportLoaderManager().restartLoader(1, null, this);
		mAdapter = new ProjectsListCursorAdapter(this);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final CheckedTextView checkedTextView = (CheckedTextView) view;
				checkedTextView.toggle();

				long projectId = ((ProjectsListCursorAdapter) parent.getAdapter()).getProjectId(position);
				final long serverId = (Long) view.getTag();
				Project project = mDb.select(serverId, projectId, null);
				project.is_sync_blocked = checkedTextView.isChecked();
				mDb.update(project);
			}
		});
		mListView.setAdapter(mAdapter);
	}

	/**
	 * Simple extension to the base {@link android.support.v4.widget.SimpleCursorAdapter} that adds {@link #getProjectId(int)} in order to retrieve
	 * the project id on the redmine server. Also, it ensures that the project is checked if it should.
	 */
	private class ProjectsListCursorAdapter extends SimpleCursorAdapter {
		public ProjectsListCursorAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_checked, null, FROM_COLUMNS, TO_VIEWS, 0);
		}

		public long getProjectId(int position) {
			if (mDataValid && mCursor != null) {
				if (mCursor.moveToPosition(position)) {
					return mCursor.getLong(mCursor.getColumnIndex(ProjectsDbAdapter.KEY_ID));
				} else {
					return 0;
				}
			} else {
				return 0;
			}
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			if (view instanceof CheckedTextView) {
				final boolean checked = cursor.getInt(cursor.getColumnIndex(ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED)) > 0;
				((CheckedTextView) view).setChecked(checked);
			}
			view.setTag(cursor.getLong(cursor.getColumnIndex(ProjectsDbAdapter.KEY_SERVER_ID)));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mDb != null) {
			mDb.close();
			mDb = null;
		}
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
		return new SimpleCursorLoader(this) {
			@Override
			public Cursor loadInBackground() {
				return getHelper().selectAllCursor(0, SELECTION_COLUMNS, null);
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
