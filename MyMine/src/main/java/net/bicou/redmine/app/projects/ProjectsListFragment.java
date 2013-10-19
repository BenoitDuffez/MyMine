package net.bicou.redmine.app.projects;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.SimpleCursorLoader;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.util.L;

import java.text.DateFormat;
import java.util.Date;

public class ProjectsListFragment extends TrackedListFragment implements LoaderCallbacks<Cursor> {
	View mFragmentView;

	private ProjectsCursorAdapter mAdapter;
	private ProjectsDbAdapter mProjectsDbAdapter;

	public static ProjectsListFragment newInstance(final Bundle args) {
		final ProjectsListFragment frag = new ProjectsListFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.d("");

		final Activity activity = getActivity();
		activity.setTitle(R.string.title_projects);

		mAdapter = new ProjectsCursorAdapter(activity, null, true);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	private ProjectsDbAdapter getHelper() {
		if (mProjectsDbAdapter == null) {
			mProjectsDbAdapter = new ProjectsDbAdapter(getActivity());
			mProjectsDbAdapter.open();
		}
		return mProjectsDbAdapter;
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		getSherlockActivity().setSupportProgressBarIndeterminate(true);
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
		return new ProjectsListCursorLoader(getActivity(), getHelper(), args);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		mAdapter.swapCursor(data);

		if (getSherlockActivity() != null) {
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(data == null);
		}

		try {
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		} catch (Exception e) {
			L.e("Couldn't update ListView... maybe the activity is not ready yet.");
		}

		if (mFragmentView != null) {
			final TextView empty = (TextView) mFragmentView.findViewById(android.R.id.empty);
			if (empty != null && data != null && data.getCount() == 0) {
				empty.setText(R.string.no_projects);
			}
		}
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mFragmentView = inflater.inflate(R.layout.frag_projects_list, container, false);
		return mFragmentView;
	}

	/**
	 * Loader that will handle the initial DB query and Cursor creation
	 *
	 * @author bicou
	 */
	public static final class ProjectsListCursorLoader extends SimpleCursorLoader {
		private final ProjectsDbAdapter mHelper;

		public ProjectsListCursorLoader(final Context context, final ProjectsDbAdapter helper, final Bundle args) {
			super(context);
			mHelper = helper;
		}

		@Override
		public Cursor loadInBackground() {
			return mHelper.selectAllCursor(0, new String[] {
					ProjectsDbAdapter.KEY_ID + " AS " + DbAdapter.KEY_ROWID,
					ProjectsDbAdapter.KEY_NAME,
					ProjectsDbAdapter.KEY_DESCRIPTION,
					// ProjectsDbAdapter.KEY_CREATED_ON,
					ProjectsDbAdapter.KEY_UPDATED_ON,
					// ProjectsDbAdapter.KEY_PARENT_ID,
					ProjectsDbAdapter.KEY_SERVER_ID,
			}, null);
		}
	}

	/**
	 * CursorAdapter that will map Cursor data to a layout
	 *
	 * @author bicou
	 */
	public final class ProjectsCursorAdapter extends CursorAdapter {
		private final Context mContext;

		public class ViewHolder {
			TextView projectName, projectDescription, projectUpdatedOn;
		}

		public ProjectsCursorAdapter(final Context context, final Cursor c, final int flags) {
			super(context, c, flags);
			mContext = context;
		}

		public ProjectsCursorAdapter(final Context context, final Cursor c, final boolean autoRequery) {
			super(context, c, autoRequery);
			mContext = context;
		}

		@Override
		public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
			final View view = LayoutInflater.from(mContext).inflate(R.layout.project_listitem, parent, false);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.projectName = (TextView) view.findViewById(R.id.project_item_name);
			viewHolder.projectDescription = (TextView) view.findViewById(R.id.project_item_description);
			viewHolder.projectUpdatedOn = (TextView) view.findViewById(R.id.project_item_updated_on);
			view.setTag(viewHolder);
			return view;
		}

		@Override
		public void bindView(final View view, final Context context, final Cursor cursor) {
			final ViewHolder viewHolder = (ViewHolder) view.getTag();
			viewHolder.projectName.setText(cursor.getString(cursor.getColumnIndex(ProjectsDbAdapter.KEY_NAME)));
			final String desc = cursor.getString(cursor.getColumnIndex(ProjectsDbAdapter.KEY_DESCRIPTION));
			if (TextUtils.isEmpty(desc)) {
				viewHolder.projectDescription.setVisibility(View.GONE);
			} else {
				viewHolder.projectDescription.setVisibility(View.VISIBLE);
				viewHolder.projectDescription.setText(desc);
			}
			final Date date = new Date(cursor.getLong(cursor.getColumnIndex(ProjectsDbAdapter.KEY_UPDATED_ON)));
			final String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(date);
			final String updatedOn = getString(R.string.project_list_item_updated_on, formattedDate);
			viewHolder.projectUpdatedOn.setText(updatedOn);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mProjectsDbAdapter != null) {
			mProjectsDbAdapter.close();
			mProjectsDbAdapter = null;
		}
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Bundle args = new Bundle();
		final Cursor c = (Cursor) mAdapter.getItem(position);
		final long projectId = c.getLong(c.getColumnIndex(DbAdapter.KEY_ROWID));
		final long serverId = c.getLong(c.getColumnIndex(ProjectsDbAdapter.KEY_SERVER_ID));
		args.putLong(Constants.KEY_PROJECT_ID, projectId);
		args.putLong(Constants.KEY_SERVER_ID, serverId);
		((ProjectsActivity) getActivity()).selectContent(args);
	}
}
