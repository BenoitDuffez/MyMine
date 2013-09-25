package net.bicou.redmine.app.issues;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;
import com.google.gson.Gson;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.issues.edit.EditIssueActivity;
import net.bicou.redmine.app.issues.edit.EditIssueFragment;
import net.bicou.redmine.app.issues.edit.ServerProjectPickerFragment;
import net.bicou.redmine.app.issues.order.IssuesOrder;
import net.bicou.redmine.app.issues.order.IssuesOrderingFragment;
import net.bicou.redmine.app.issues.order.IssuesOrderingFragment.IssuesOrderSelectionListener;
import net.bicou.redmine.app.misc.EmptyFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.net.upload.IssueSerializer;
import net.bicou.redmine.net.upload.JsonUploader;
import net.bicou.redmine.net.upload.ObjectSerializer;
import net.bicou.redmine.sync.IssuesSyncAdapterService;
import net.bicou.redmine.util.L;
import net.bicou.splitactivity.SplitActivity;

import java.util.List;

public class IssuesActivity extends SplitActivity<IssuesListFragment, IssueFragment> implements AsyncTaskFragment.TaskFragmentCallbacks,
		ServerProjectPickerFragment.ServerProjectSelectionListener {
	int mNavMode;
	IssuesOrder mCurrentOrder;
	public static final int ACTION_REFRESH_ISSUES = 0;
	public static final int ACTION_ISSUE_LOAD_ISSUE = 1;
	public static final int ACTION_ISSUE_LOAD_OVERVIEW = 2;
	public static final int ACTION_ISSUE_LOAD_ATTACHMENTS = 3;
	public static final int ACTION_DELETE_ISSUE = 4;
	public static final int ACTION_UPLOAD_ISSUE = 5;

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

		final Bundle args = new Bundle(), extras;
		if ((extras = intent.getExtras()) != null) {
			args.putAll(extras);
		}

		mNavMode = ActionBar.NAVIGATION_MODE_LIST;

		// Get the intent, verify the action and get the query
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String query = intent.getStringExtra(SearchManager.QUERY);
			new IssuesListFilter(query).saveTo(args);
			mNavMode = ActionBar.NAVIGATION_MODE_STANDARD;
		}

		// Get issue sort order
		if (!args.keySet().contains(IssuesOrder.KEY_HAS_COLUMNS_ORDER)) {
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
		}

		saveMainFragmentState(args);

		return args;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);

		super.onCreate(savedInstanceState);
		AsyncTaskFragment.attachAsyncTaskFragment(this);

		Bundle args = getIntent().getExtras();
		if (args != null && args.containsKey(Constants.KEY_ISSUE_ID)) {
			selectContent(args);
		}
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
		L.d("");
		supportInvalidateOptionsMenu();
	}

	private void showServerProjectPickerDialog() {
		DialogFragment newFragment = ServerProjectPickerFragment.newInstance();
		newFragment.show(getSupportFragmentManager(), "serverProjectPicker");
	}

	@Override
	public void onServerProjectPicked(final Server server, final Project project) {
		if (server != null && server.rowId > 0 && project != null && project.id > 0) {
			Intent intent = new Intent(this, EditIssueActivity.class);
			intent.putExtra(Constants.KEY_SERVER, server);
			intent.putExtra(Constants.KEY_PROJECT, project);
			startActivityForResult(intent, 0);
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			AsyncTaskFragment.runTask(this, ACTION_UPLOAD_ISSUE, data.getExtras());
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		IssueFragment content = getContentFragment();
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;

		case R.id.menu_issues_add:
			showServerProjectPickerDialog();
			return true;

		case R.id.menu_issue_edit:
			if (content != null) {
				Issue issue = content.getIssue();
				if (issue != null) {
					String json = new Gson().toJson(issue, Issue.class);
					Intent intent = new Intent(this, EditIssueActivity.class);
					intent.putExtra(IssueFragment.KEY_ISSUE_JSON, json);
					startActivityForResult(intent, 0);
				}
			}
			return true;

		case R.id.menu_issue_delete:
			if (content != null) {
				final Issue issue = content.getIssue();
				if (issue != null && issue.server != null && issue.id > 0) {
					new AlertDialog.Builder(this).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							AsyncTaskFragment.runTask(IssuesActivity.this, ACTION_DELETE_ISSUE, issue);
							dialog.dismiss();
						}
					}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							dialog.dismiss();
						}
					}).setMessage(getString(R.string.issue_delete_confirm_dialog)).show();
				}
			}
			return true;

		case R.id.menu_issue_browser:
			if (content != null) {
				final Issue issue = content.getIssue();
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
		int lastPosition = -1;

		@Override
		public boolean onNavigationItemSelected(final int itemPosition, final long itemId) {
			L.d("pos=" + itemPosition);
			if (lastPosition >= 0) {
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
	public Object doInBackGround(Context applicationContext, final int action, final Object parameters) {
		Issue issue;
		String uri;

		switch (action) {
		case ACTION_REFRESH_ISSUES:
			IssuesSyncAdapterService.Synchronizer synchronizer = new IssuesSyncAdapterService.Synchronizer(applicationContext);
			ServersDbAdapter db = new ServersDbAdapter(applicationContext);
			db.open();
			for (Server server : db.selectAll()) {
				synchronizer.synchronizeIssues(server, null, 0);
			}
			db.close();
			break;

		case ACTION_ISSUE_LOAD_ISSUE:
			return IssueOverviewFragment.loadIssue(applicationContext, (Bundle) parameters);

		case ACTION_ISSUE_LOAD_OVERVIEW:
			return IssueOverviewFragment.loadIssueOverview(applicationContext, (Issue) parameters);

		case ACTION_ISSUE_LOAD_ATTACHMENTS:
			return IssueOverviewFragment.loadIssueAttachments(applicationContext, (Issue) parameters);

		case ACTION_DELETE_ISSUE:
			issue = (Issue) parameters;
			IssueSerializer serializer = new IssueSerializer(this, issue, null, true);
			uri = "issues/" + issue.id + ".json";
			return new JsonUploader().uploadObject(this, issue.server, uri, serializer);

		case ACTION_UPLOAD_ISSUE:
			Bundle params = (Bundle) parameters;
			issue = new Gson().fromJson(params.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
			IssueSerializer issueSerializer = new IssueSerializer(applicationContext, issue, params.getString(EditIssueFragment.KEY_ISSUE_NOTES));
			if (issue.id <= 0 || issueSerializer.getRemoteOperation() == ObjectSerializer.RemoteOperation.ADD) {
				uri = "issues.json";
			} else {
				uri = "issues/" + issue.id + ".json";
			}
			return new JsonUploader().uploadObject(applicationContext, issue.server, uri, issueSerializer);
		}

		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Crouton.cancelAllCroutons();
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

		case ACTION_ISSUE_LOAD_ISSUE:
		case ACTION_ISSUE_LOAD_OVERVIEW:
		case ACTION_ISSUE_LOAD_ATTACHMENTS:
			IssueFragment content = getContentFragment();
			if (content != null) {
				Fragment frag = content.getFragmentFromViewPager(0);
				if (frag != null && frag instanceof IssueOverviewFragment) {
					switch (action) {
					case ACTION_ISSUE_LOAD_ISSUE:
						((IssueOverviewFragment) frag).onIssueLoaded(result);
						break;

					case ACTION_ISSUE_LOAD_ATTACHMENTS:
						if (result instanceof JsonNetworkError) {
							((IssueOverviewFragment) frag).onNetworkError((JsonNetworkError) result);
						} else {
							((IssueOverviewFragment) frag).onIssueOverviewLoaded((String) result);
						}
						break;

					case ACTION_ISSUE_LOAD_OVERVIEW:
						((IssueOverviewFragment) frag).onIssueOverviewLoaded((String) result);
						break;
					}
				}
			}
			break;

		case ACTION_DELETE_ISSUE:
			L.d("delete issue: " + result);
			if (result == null || !(result instanceof JsonNetworkError)) {
				IssuesDbAdapter db = new IssuesDbAdapter(this);
				db.open();
				db.delete((Issue) parameters);
				db.close();
				Crouton.makeText(this, getString(R.string.issue_delete_confirmed), Style.CONFIRM).show();
			} else {
				((JsonNetworkError) result).displayCrouton(this, null);
			}
			break;


		case ACTION_UPLOAD_ISSUE:
			if (result instanceof JsonNetworkError) {
				Crouton.makeText(this, String.format(getString(R.string.issue_upload_failed), ((JsonNetworkError) result).getMessage(this)), Style.ALERT).show();
			} else {
				Crouton.makeText(this, getString(R.string.issue_upload_successful), Style.CONFIRM).show();
			}
			L.d("Upload issue json: " + result);
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
