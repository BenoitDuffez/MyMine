package net.bicou.redmine.app.issues;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.app.issues.order.IssuesOrder;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.splitactivity.SplitActivity;

public class IssuesListFragment extends TrackedListFragment implements LoaderCallbacks<Cursor> {
	View mFragmentView;

	private IssuesListCursorAdapter mAdapter;
	private IssuesDbAdapter mIssuesDbAdapter;

	boolean mHasSearchQuery;

	IssuesListFilter mFilter;
	IssuesOrder mIssuesOrder;

	public static IssuesListFragment newInstance(final Bundle args) {
		final IssuesListFragment frag = new IssuesListFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = ((SplitActivity) getActivity()).getMainFragmentPreviousState();
		// Restore from backstack
		if (args != null) {
			mIssuesOrder = IssuesOrder.fromBundle(args);
			mFilter = IssuesListFilter.fromBundle(args);
			//here, the activity won't get a notification from the action bar navigation list spinner selection, so we need to launch the loader ourselves
			getLoaderManager().initLoader(0, args, this);
		}
		// Restore from saved instance state
		else if (savedInstanceState != null) {
			mFilter = IssuesListFilter.fromBundle(savedInstanceState);
			mIssuesOrder = IssuesOrder.fromBundle(savedInstanceState);
			getLoaderManager().initLoader(0, args, this);
		}
		// Retrieve from fragment arguments
		else {
			args = getArguments();
			mIssuesOrder = IssuesOrder.fromBundle(args);
			mFilter = IssuesListFilter.fromBundle(args);

			if (mFilter.type == IssuesListFilter.FilterType.SEARCH) {
				// a search doesn't trigger a notification from the action bar navigation list spinner selection, so we need to launch the loader ourselves
				getLoaderManager().initLoader(0, args, this);
			}
		}

		mAdapter = new IssuesListCursorAdapter(getActivity(), null, true);
		setListAdapter(mAdapter);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mFragmentView = inflater.inflate(R.layout.frag_issues_list, container, false);
		return mFragmentView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Bundle args = new Bundle();
		saveInstance(args);
		((SplitActivity) getActivity()).saveMainFragmentState(args);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveInstance(outState);
	}

	private void saveInstance(Bundle outState) {
		if (mIssuesOrder != null) {
			mIssuesOrder.saveTo(outState);
		}
		if (mFilter != null) {
			mFilter.saveTo(outState);
		}
	}

	public void updateFilter(final IssuesListFilter filter) {
		mFilter = filter;
		restartLoader();
	}

	public void updateColumnsOrder(final IssuesOrder order) {
		mIssuesOrder = order;
		restartLoader();
	}

	public void refreshList() {
		restartLoader();
	}

	private void restartLoader() {
		final Bundle args = new Bundle();
		if (mFilter != null) {
			mFilter.saveTo(args);
		}
		if (mIssuesOrder != null && !mIssuesOrder.isEmpty()) {
			mIssuesOrder.saveTo(args);
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
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminate(true);
		((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
		return new IssuesListCursorLoader(getActivity(), getHelper(), args);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		mAdapter.swapCursor(data);
		if (getActivity() != null) {
			((ActionBarActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(data == null);
		}

		final TextView empty = (TextView) mFragmentView.findViewById(android.R.id.empty);
		if (empty != null && (data == null || data.getCount() == 0)) {
			if (mHasSearchQuery) {
				empty.setText(R.string.issues_list_empty_search);
			} else {
				empty.setText(R.string.issues_list_empty);
			}
		}
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
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
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Cursor c = (Cursor) mAdapter.getItem(position);
		final long issueId = c.getLong(c.getColumnIndex(DbAdapter.KEY_ROWID));
		final long serverId = c.getLong(c.getColumnIndex(IssuesDbAdapter.KEY_SERVER_ID));

		final Bundle args = new Bundle();
		args.putLong(Constants.KEY_ISSUE_ID, issueId);
		args.putLong(Constants.KEY_SERVER_ID, serverId);

		((IssuesActivity) getActivity()).selectContent(args);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_issues_list, menu);
	}
}
