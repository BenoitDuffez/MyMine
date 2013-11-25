package net.bicou.redmine.app.roadmap;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.issues.IssuesListCursorAdapter;
import net.bicou.redmine.app.issues.IssuesListCursorLoader;
import net.bicou.redmine.app.issues.IssuesListFilter;
import net.bicou.redmine.app.issues.order.IssuesOrder;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.util.Util;

/**
 * Created by bicou on 28/05/13.
 */
public class RoadmapFragment extends TrackedListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	TextView mVersionName, mDueDate, mPercentComplete, mIssuesCount, mDescription;
	Button mShowWikiButton;
	ProgressBar mProgressBar;
	Version mVersion;
	View mFragmentView;

	IssuesDbAdapter mIssuesDbAdapter;

	IssuesListCursorAdapter mAdapter;

	long mCurrentServerId;
	IssuesOrder mCurrentOrder;
	IssuesListFilter mFilter;

	public static final String KEY_VERSION_JSON = "net.bicou.redmine.app.roadmap.Version";

	public static RoadmapFragment newInstance(Bundle args) {
		RoadmapFragment frag = new RoadmapFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mFragmentView = inflater.inflate(R.layout.frag_roadmap, container, false);

		setHasOptionsMenu(true);

		mVersionName = (TextView) mFragmentView.findViewById(R.id.roadmap_version);
		mDueDate = (TextView) mFragmentView.findViewById(R.id.roadmap_due_date);
		mPercentComplete = (TextView) mFragmentView.findViewById(R.id.roadmap_percent_complete);
		mIssuesCount = (TextView) mFragmentView.findViewById(R.id.roadmap_issue_count);
		mDescription = (TextView) mFragmentView.findViewById(R.id.roadmap_version_description);
		mShowWikiButton = (Button) mFragmentView.findViewById(R.id.roadmap_show_wiki);
		mProgressBar = (ProgressBar) mFragmentView.findViewById(R.id.roadmap_progress_bar);

		String json = getArguments().getString(KEY_VERSION_JSON);
		if (!TextUtils.isEmpty(json)) {
			mVersion = new Gson().fromJson(json, Version.class);
			if (mVersion != null) {
				mVersionName.setText(mVersion.name);
				mDueDate.setText(getString(R.string.roadmap_due_date, DateFormat.getLongDateFormat(getActivity()).format(mVersion.due_date.getTime())));

				if (TextUtils.isEmpty(mVersion.description)) {
					mDescription.setVisibility(View.GONE);
				} else {
					mDescription.setVisibility(View.VISIBLE);
					mDescription.setText(mVersion.description);
				}

				mAdapter = new IssuesListCursorAdapter(getActivity(), null, true, new IssuesListCursorAdapter.IssueFavoriteToggleListener() {
					@Override
					public void onIssueFavoriteChanged(long serverId, long issueId, boolean isFavorite) {
						Bundle args = new Bundle();
						args.putLong(Constants.KEY_SERVER_ID, serverId);
						args.putLong(Constants.KEY_ISSUE_ID, issueId);
						args.putBoolean(IssuesDbAdapter.KEY_IS_FAVORITE, isFavorite);
						AsyncTaskFragment.runTask((ActionBarActivity) getActivity(), RoadmapActivity.ACTION_ISSUE_TOGGLE_FAVORITE, args);
					}
				});
				setListAdapter(mAdapter);

				// Versions Wiki page is not implemented in Redmine
				// http://www.redmine.org/issues/10384
				// TODO: fix or alert users
				mShowWikiButton.setVisibility(View.GONE);

				// Get issue sort order
				if (mCurrentOrder == null) {
					if (savedInstanceState == null) {
						mCurrentOrder = IssuesOrder.fromPreferences(getActivity());
					} else {
						mCurrentOrder = IssuesOrder.fromBundle(savedInstanceState);
					}
				}

				new FillViewsTask().execute();

				((ActionBarActivity) getActivity()).setSupportProgressBarVisibility(true);
				//	setHasOptionsMenu(true); TODO: disabled on purpose: maybe not that useful
			}
		} else {
			throw new IllegalStateException("Can't use " + this.getClass().getSimpleName() + " without sending the " + KEY_VERSION_JSON + " as a parameter");
		}

		return mFragmentView;
	}

	private class FillViewsTask extends AsyncTask<Void, Void, Void> {
		IssuesDbAdapter db;
		int nbOpen = 0, nbClosed = 0, percentComplete = 0;

		public FillViewsTask() {
			db = new IssuesDbAdapter(getActivity());
			db.open();
		}

		@Override
		protected void onPreExecute() {
			db.open();
		}

		private String buildSql() {
			String[] cols = { IssueStatusesDbAdapter.TABLE_ISSUE_STATUSES + "." + IssueStatusesDbAdapter.KEY_IS_CLOSED, IssuesDbAdapter.TABLE_ISSUES + "." + IssuesDbAdapter.KEY_DONE_RATIO, IssuesDbAdapter.TABLE_ISSUES + "." + IssuesDbAdapter.KEY_ID, };

			String[] tables = { IssuesDbAdapter.TABLE_ISSUES, };

			String sql = "SELECT " + Util.join(cols, ", ");
			sql += " FROM " + Util.join(tables, ", ");
			sql += " LEFT JOIN " + IssueStatusesDbAdapter.TABLE_ISSUE_STATUSES //
					+ " ON " + IssueStatusesDbAdapter.TABLE_ISSUE_STATUSES + "." + IssueStatusesDbAdapter.KEY_ID //
					+ " = " + IssuesDbAdapter.TABLE_ISSUES + "." + IssuesDbAdapter.KEY_STATUS_ID;
			sql += " WHERE " + IssuesDbAdapter.KEY_FIXED_VERSION_ID + " = " + mVersion.id;

			return sql;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			Cursor c = db.rawQuery(buildSql(), null);
			while (c.moveToNext()) {
				if (c.getInt(c.getColumnIndex(IssueStatusesDbAdapter.KEY_IS_CLOSED)) == 0) {
					nbOpen++;
					percentComplete += c.getInt(c.getColumnIndex(IssuesDbAdapter.KEY_DONE_RATIO));
				} else {
					percentComplete += 100;
					nbClosed++;
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			db.close();
			int realProgress = 0, rawProgress = 0;
			if (nbOpen + nbClosed != 0) {
				double progress = 0.01 * percentComplete / (nbOpen + nbClosed);
				realProgress = (int) (100 * progress);
				rawProgress = 100 * nbClosed / (nbOpen + nbClosed);
			}

			mProgressBar.setProgress(rawProgress);
			mProgressBar.setSecondaryProgress(realProgress);
			mProgressBar.setVisibility(View.VISIBLE);
			mPercentComplete.setText(String.format("%d %%", realProgress));

			mIssuesCount.setText(getString(R.string.roadmap_issues_count, nbOpen + nbClosed, nbOpen));

			((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
		}

		@Override
		protected void onCancelled() {
			db.close();
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof RoadmapsListFragment.CurrentProjectInfo) {
			RoadmapsListFragment.CurrentProjectInfo info = (RoadmapsListFragment.CurrentProjectInfo) activity;
			mCurrentServerId = info.getCurrentProject() == null ? 0 : info.getCurrentProject().server == null ? 0 : info.getCurrentProject().server.rowId;
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Bundle args = new Bundle();
		mFilter = new IssuesListFilter(mCurrentServerId, IssuesListFilter.FilterType.VERSION, mVersion.id);
		args.putBoolean(IssuesListFilter.KEY_HAS_FILTER, true);
		mFilter.saveTo(args);

		if (mCurrentOrder != null) {
			mCurrentOrder.saveTo(args);
		}

		getLoaderManager().initLoader(0, args, this);
	}

	public IssuesOrder getCurrentOrder() {
		return mCurrentOrder;
	}

	public void setNewIssuesOrder(IssuesOrder newOrder) {
		mCurrentOrder = newOrder;
		if (mCurrentOrder != null) {
			mCurrentOrder.saveToPreferences(getActivity());
		}

		restartLoader();
	}


	private void restartLoader() {
		final Bundle args = new Bundle();
		if (mCurrentOrder != null) {
			mCurrentOrder.saveTo(args);
		}
		if (mFilter != null) {
			args.putBoolean(IssuesListFilter.KEY_HAS_FILTER, true);
			mFilter.saveTo(args);
		}
		getLoaderManager().restartLoader(0, args, this);
	}

	private IssuesDbAdapter getHelper() {
		if (mIssuesDbAdapter == null) {
			mIssuesDbAdapter = new IssuesDbAdapter(getActivity());
			mIssuesDbAdapter.open();
		}
		return mIssuesDbAdapter;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mIssuesDbAdapter != null) {
			mIssuesDbAdapter.close();
			mIssuesDbAdapter = null;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminate(true);
		((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
		return new IssuesListCursorLoader(getActivity(), getHelper(), args);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		mAdapter.swapCursor(data);
		((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(data == null);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		}

		final TextView empty = (TextView) mFragmentView.findViewById(android.R.id.empty);
		if (empty != null && (data == null || data.getCount() == 0)) {
			empty.setText(R.string.issues_list_empty);
		}
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Cursor c = (Cursor) mAdapter.getItem(position);
		final long issueId = c.getLong(c.getColumnIndex(DbAdapter.KEY_ROWID));
		final long serverId = c.getLong(c.getColumnIndex(IssuesDbAdapter.KEY_SERVER_ID));

		final Bundle args = new Bundle();
		args.putLong(Constants.KEY_ISSUE_ID, issueId);
		args.putLong(Constants.KEY_SERVER_ID, serverId);

		Intent intent = new Intent(getActivity(), IssuesActivity.class);
		intent.putExtras(args);
		startActivity(intent);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_roadmap, menu);
	}
}
