package net.bicou.redmine.app.issues;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;
import net.bicou.android.splitscreen.SplitActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.issues.order.IssuesOrder;
import net.bicou.redmine.app.issues.order.IssuesOrderingFragment;
import net.bicou.redmine.app.issues.order.IssuesOrderingFragment.IssuesOrderSelectionListener;
import net.bicou.redmine.app.misc.EmptyFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.sync.IssuesSyncAdapterService;
import net.bicou.redmine.util.L;

import java.util.List;

public class IssuesActivity extends SplitActivity<IssuesListFragment, IssueFragment> implements AsyncTaskFragment.TaskFragmentCallbacks {
	int mNavMode;
	IssuesOrder mCurrentOrder;
	public static final int ACTION_REFRESH_ISSUES = 0;
	public static final int ACTION_ISSUE_LOAD_OVERVIEW = 1;
	public static final int ACTION_ISSUE_LOAD_ATTACHMENTS= 2;

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
		return EmptyFragment.newInstance(R.drawable.issues_empty_fragment);
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
				mCurrentOrder = IssuesOrder.fromPreferences(this);
			} else {
				mCurrentOrder = IssuesOrder.fromBundle(savedInstanceState);
			}
		}

		if (mCurrentOrder != null) {
			mCurrentOrder.saveTo(args);
		}

		return args;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);

		super.onCreate(savedInstanceState);
		AsyncTaskFragment.attachAsyncTaskFragment(this);
	}

	void saveNewColumnsOrder(final IssuesOrder orderColumns) {
		mCurrentOrder = orderColumns;
		if (mCurrentOrder != null) {
			mCurrentOrder.saveToPreferences(this);
		}
	}

	GetNavigationSpinnerDataTask.NavigationModeAdapterCallback mNavigationModeAdapterCallback = new GetNavigationSpinnerDataTask.NavigationModeAdapterCallback() {
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
			new GetNavigationSpinnerDataTask(this, mNavigationModeAdapterCallback).execute();
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;

		case R.id.menu_issue_browser:
			IssueFragment frag = getContentFragment();
			if (frag != null) {
				final Issue issue = frag.getIssue();
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
				public void onOrderColumnsSelected(final IssuesOrder orderColumns) {
					saveNewColumnsOrder(orderColumns);

					IssuesListFragment list = getMainFragment();
					if (list != null) {
						list.updateColumnsOrder(orderColumns);
					} else {
						Bundle args = getMainFragmentArgs(null);
						if (orderColumns != null) {
							orderColumns.saveTo(args);
						}
						showMainFragment(args);
					}
				}
			});
			issuesOrder.show(getSupportFragmentManager(), "issues_order");
			return true;

		case R.id.menu_issues_refresh:
			AsyncTaskFragment.runTask(this, ACTION_REFRESH_ISSUES, null);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_issues, menu);

		final SearchView searchView = (SearchView) menu.findItem(R.id.menu_issues_search).getActionView();
		if (searchView != null) {
			final SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		}

		return super.onCreateOptionsMenu(menu);
	}

	OnNavigationListener mNavigationCallbacks = new OnNavigationListener() {
		int lastPosition;

		@Override
		public boolean onNavigationItemSelected(final int itemPosition, final long itemId) {
			L.d("pos=" + itemPosition);
			if (!mSpinnerAdapter.isSeparator(itemPosition)) {
				IssuesListFragment list = getMainFragment();
				IssuesListFilter newFilter = mSpinnerAdapter.getFilter(itemPosition);
				if (list != null) {
					list.updateFilter(newFilter);
				} else {
					Bundle args = getMainFragmentArgs(null);
					if (newFilter != null) {
						newFilter.saveTo(args);
					}
					showMainFragment(args);
				}
			}
			lastPosition = itemPosition;
			return true;
		}
	};

	IssuesMainFilterAdapter mSpinnerAdapter;

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final int action, final Object parameters) {
		switch (action) {
		case ACTION_REFRESH_ISSUES:
			IssuesSyncAdapterService.Synchronizer synchronizer = new IssuesSyncAdapterService.Synchronizer(this);
			ServersDbAdapter db = new ServersDbAdapter(this);
			db.open();
			for (Server server : db.selectAll()) {
				synchronizer.synchronizeIssues(server, null, 0);
			}
			db.close();
			break;

		case ACTION_ISSUE_LOAD_OVERVIEW:
		case ACTION_ISSUE_LOAD_ATTACHMENTS:
			IssueFragment content = getContentFragment();
			if (content != null) {
				Fragment frag = content.getFragmentFromViewPager(0);
				if (frag != null && frag instanceof IssueOverviewFragment) {
					IssueOverviewFragment overview = (IssueOverviewFragment) frag;
					if (action==ACTION_ISSUE_LOAD_OVERVIEW){
						return overview.loadIssueOverview();
					}else{
						return overview.loadIssueAttachments();
					}
				}
			}
			break;
		}

		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		switch (action) {
		case ACTION_REFRESH_ISSUES:
			if (getMainFragment() != null) {
				getMainFragment().refreshList();
			}
			break;

		case ACTION_ISSUE_LOAD_OVERVIEW:
			IssueFragment content = getContentFragment();
			if (content != null) {
				Fragment frag = content.getFragmentFromViewPager(0);
				if (frag != null && frag instanceof IssueOverviewFragment) {
					((IssueOverviewFragment) frag).onIssueOverviewLoaded((String) result);
				}
			}
			break;
		}
	}

	public static class GetNavigationSpinnerDataTask extends AsyncTask<Void, Void, IssuesMainFilterAdapter> {
		Context mContext;
		NavigationModeAdapterCallback mCallback;

		public interface NavigationModeAdapterCallback {
			public void onNavigationModeAdapterReady(IssuesMainFilterAdapter adapter);
		}

		GetNavigationSpinnerDataTask(final Context ctx, final NavigationModeAdapterCallback cb) {
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
