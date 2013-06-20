package net.bicou.redmine.app.issues;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.google.gson.Gson;
import net.bicou.android.splitscreen.SplitActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter.OrderColumn;
import net.bicou.redmine.app.issues.IssuesOrderingFragment.IssuesOrderSelectionListener;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.util.PreferencesManager;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;

public class IssuesActivity extends SplitActivity<IssuesListFragment, IssueFragment> {
	int mNavMode;
	ArrayList<OrderColumn> mCurrentOrder;

	@Override
	protected IssuesListFragment createMainFragment(Bundle args) {
		return IssuesListFragment.newInstance(args);
	}

	@Override
	protected IssueFragment createContentFragment(Bundle args) {
		return IssueFragment.newInstance(args);
	}

	@Override
	protected Fragment createEmptyFragment(Bundle args) {
		return new EmptyFragment();
	}

	private static class EmptyFragment extends SherlockFragment{
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v=inflater.inflate(R.layout.frag_empty,container,false);
			v.setBackgroundResource(R.drawable.issues_empty_fragment);
			return v;
		}
	}

	@Override
	public Bundle getMainFragmentArgs(Bundle savedInstanceState) {
		final Intent intent = getIntent();

		final Bundle args, extras;
		if ((extras = intent.getExtras()) != null) {
			args = new Bundle(extras);
		} else {
			args = new Bundle();
		}

		mNavMode = ActionBar.NAVIGATION_MODE_LIST;

		// Get the intent, verify the action and get the query
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String query = intent.getStringExtra(SearchManager.QUERY);
			// searchIssues(query);
			args.putBoolean(IssuesListFilter.KEY_HAS_FILTER, true);
			new IssuesListFilter(query).saveTo(args);
			mNavMode = ActionBar.NAVIGATION_MODE_STANDARD;
		}

		// Get issue sort order
		if (mCurrentOrder == null) {
			if (savedInstanceState == null) {
				mCurrentOrder = Util.getPreferredIssuesOrder(this);
			} else {
				mCurrentOrder = savedInstanceState.getParcelableArrayList(IssuesOrderingFragment.KEY_COLUMNS_ORDER);
			}
		}
		args.putParcelableArrayList(IssuesOrderingFragment.KEY_COLUMNS_ORDER, mCurrentOrder);

		return args;
	}

	void saveNewColumnsOrder(final ArrayList<OrderColumn> orderColumns) {
		mCurrentOrder = orderColumns;
		final String json = new Gson().toJson(mCurrentOrder, IssuesOrderingFragment.ORDER_TYPE);
		PreferencesManager.setString(IssuesActivity.this, IssuesOrderingFragment.KEY_COLUMNS_ORDER, json);
	}

	GetNavigationModeAdapterTask.NavigationModeAdapterCallback mNavigationModeAdapterCallback = new GetNavigationModeAdapterTask.NavigationModeAdapterCallback
			() {
		@Override
		public void onNavigationModeAdapterReady(final IssuesMainFilterAdapter adapter) {
			mSpinnerAdapter = adapter;
			final ActionBar ab = getSupportActionBar();
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			ab.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallbacks);
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		if (mNavMode == ActionBar.NAVIGATION_MODE_LIST) {
			new GetNavigationModeAdapterTask(this, mNavigationModeAdapterCallback).execute();
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = android.R.id.content;
		final Fragment frag = getSupportFragmentManager().findFragmentById(id);

		switch (item.getItemId()) {
		/*case android.R.id.home:
			finish();
			return true;
		*/
		case R.id.menu_issue_browser:
			if (frag instanceof IssueFragment) {
				final Issue issue = ((IssueFragment) frag).getIssue();
				if (issue != null) {
					String url = issue.server.serverUrl;
					if (!url.endsWith("/")) {
						url += "/";
					}
					url += "issues/" + issue.id;

					final Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				}
			} else {
				supportInvalidateOptionsMenu();
			}
			return true;

		case R.id.menu_issues_sort:
			final IssuesOrderingFragment issuesOrder = IssuesOrderingFragment.newInstance(mCurrentOrder);
			issuesOrder.setOrderSelectionListener(new IssuesOrderSelectionListener() {
				@Override
				public void onOrderColumnsSelected(final ArrayList<OrderColumn> orderColumns) {
					saveNewColumnsOrder(orderColumns);

					final FragmentManager fm = getSupportFragmentManager();
					final Fragment frag = fm.findFragmentById(android.R.id.content);
					if (frag instanceof IssuesListFragment) {
						((IssuesListFragment) frag).updateColumnsOrder(orderColumns);
					} else {
						// TODO?
					}
				}
			});
			issuesOrder.show(getSupportFragmentManager(), "issues_order");
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_issues, menu);

		final SearchView searchView = (SearchView) menu.findItem(R.id.menu_issues_search).getActionView();
		if (searchView != null) {
			final SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

			final View searchPlate = searchView.findViewById(R.id.abs__search_plate);
			searchPlate.setBackgroundResource(R.drawable.issues_search_background);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(IssuesOrderingFragment.KEY_COLUMNS_ORDER, mCurrentOrder);
	}

	OnNavigationListener mNavigationCallbacks = new OnNavigationListener() {
		int lastPosition;

		@Override
		public boolean onNavigationItemSelected(final int itemPosition, final long itemId) {
			final FragmentManager fm = getSupportFragmentManager();
			final Fragment frag = fm.findFragmentById(android.R.id.content);
			if (mSpinnerAdapter.isSeparator(itemPosition)) {
				getSupportActionBar().setSelectedNavigationItem(lastPosition);
			} else {
				if (frag instanceof IssuesListFragment) {
					((IssuesListFragment) frag).updateFilter(mSpinnerAdapter.getFilter(itemPosition));
				} else {
					// TODO??
				}
			}
			lastPosition = itemPosition;
			return true;
		}
	};

	IssuesMainFilterAdapter mSpinnerAdapter;

	public static class GetNavigationModeAdapterTask extends AsyncTask<Void, Void, IssuesMainFilterAdapter> {
		Context mContext;
		NavigationModeAdapterCallback mCallback;

		public interface NavigationModeAdapterCallback {
			public void onNavigationModeAdapterReady(IssuesMainFilterAdapter adapter);
		}

		GetNavigationModeAdapterTask(final Context ctx, final NavigationModeAdapterCallback cb) {
			mContext = ctx;
			mCallback = cb;
		}

		@Override
		protected IssuesMainFilterAdapter doInBackground(final Void... params) {
			final QueriesDbAdapter qdb = new QueriesDbAdapter(mContext);
			qdb.open();
			final List<Query> queries = qdb.selectAll(null);
			qdb.close();

			final ProjectsDbAdapter pdb = new ProjectsDbAdapter(mContext);
			pdb.open();
			final List<Project> projects = pdb.selectAll();
			pdb.close();

			return new IssuesMainFilterAdapter(mContext, queries, projects);
		}

		@Override
		protected void onPostExecute(final IssuesMainFilterAdapter result) {
			if (mCallback != null) {
				mCallback.onNavigationModeAdapterReady(result);
			}
		}
	}
}
