package net.bicou.redmine.app.issues;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter.OrderColumn;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;

import java.util.ArrayList;

public class IssuesListFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {//TODO }, SplitScreenFragmentConfigurationChangesListener {
	View mFragmentView;

	private IssuesListCursorAdapter mAdapter;
	private IssuesDbAdapter mIssuesDbAdapter;

	//TODO boolean mIsSplitScreen;
	boolean mHasFilter;
	boolean mHasSearchQuery;

	IssuesListFilter mFilter;
	ArrayList<OrderColumn> mColumnsOrder;

	public static IssuesListFragment newInstance(final Bundle args) {
		final IssuesListFragment frag = new IssuesListFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle args = getArguments();
		//TODO mIsSplitScreen = args.getBoolean(SplitScreenBehavior.KEY_IS_SPLIT_SCREEN);
		mHasFilter = args.getBoolean(IssuesListFilter.KEY_HAS_FILTER, false);
		mHasSearchQuery = args.containsKey(IssuesOrderingFragment.KEY_COLUMNS_ORDER);
		final boolean emptyLoader = mHasFilter == false && mHasSearchQuery == false;

		final Activity activity = getActivity();
		activity.setTitle(R.string.title_issues);

		mAdapter = new IssuesListCursorAdapter(activity, null, true);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, emptyLoader ? null : args, this);

		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		final int navMode = mHasFilter ? ActionBar.NAVIGATION_MODE_STANDARD : ActionBar.NAVIGATION_MODE_LIST;
		getSherlockActivity().getSupportActionBar().setNavigationMode(navMode);
	}

	public void updateFilter(final IssuesListFilter filter) {
		mFilter = filter;
		restartLoader();
	}

	public void updateColumnsOrder(final ArrayList<OrderColumn> order) {
		mColumnsOrder = order;
		restartLoader();
	}

	private void restartLoader() {
		final Bundle args = new Bundle();
		if (mFilter != null) {
			mFilter.saveTo(args);
		}
		if (mColumnsOrder != null && mColumnsOrder.size() > 0) {
			args.putParcelableArrayList(IssuesOrderingFragment.KEY_COLUMNS_ORDER, mColumnsOrder);
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
		getSherlockActivity().setSupportProgressBarIndeterminate(true);
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
		return new IssuesListCursorLoader(getActivity(), getHelper(), args);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		mAdapter.swapCursor(data);
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(data == null);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
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
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mFragmentView = inflater.inflate(R.layout.frag_issues_list, container, false);
		return mFragmentView;
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

		final IssueFragment frag = IssueFragment.newInstance(args);

		// Open issue in the right pane
		// TODO
		//	if (mIsSplitScreen) {
		//		getFragmentManager().beginTransaction().replace(android.R.id.content, frag).commit();
		//	} else {
		getSherlockActivity().getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		getFragmentManager().beginTransaction().replace(android.R.id.content, frag).addToBackStack("prout").commit();
		//	}
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_issues_list, menu);
	}
}
