package net.bicou.redmine.app.misc;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.drawers.DrawerActivity;
import net.bicou.redmine.app.drawers.main.DrawerMenuFragment;
import net.bicou.redmine.app.issues.edit.EditIssueActivity;
import net.bicou.redmine.app.issues.edit.IssueUploader;
import net.bicou.redmine.app.issues.edit.ServerProjectPickerFragment;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.app.welcome.OverviewCard;
import net.bicou.redmine.app.welcome.WelcomeFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends DrawerActivity implements ServerProjectPickerFragment.ServerProjectSelectionListener, AsyncTaskFragment.TaskFragmentCallbacks {
	private static final int ACTION_LOAD_ACTIVITY = 0;
	private static final int ACTION_UPLOAD_ISSUE = 1;
	public static final int ACTION_REFRESH_MAIN_SCREEN = 2;

	private static final String CAMPAIGN_SOURCE_PARAM = "utm_source";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content, LoadingFragment.newInstance()).commit();
		getSupportFragmentManager().beginTransaction().replace(R.id.navigation_drawer, new DrawerMenuFragment()).commit();

		AsyncTaskFragment.attachAsyncTaskFragment(this);

		EasyTracker.getInstance(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);

		// Get the intent that started this Activity.
		Intent intent = this.getIntent();
		Uri uri = intent.getData();

		// Send a screenview using any available campaign or referrer data.
		MapBuilder.createAppView().setAll(getReferrerMapFromUri(uri));
	}

	/**
	 * Given a URI, returns a map of campaign data that can be sent with
	 * any GA hit.
	 *
	 * @param uri A hierarchical URI that may or may not have campaign data
	 *            stored in query parameters.
	 *
	 * @return A map that may contain campaign or referrer
	 * that may be sent with any Google Analytics hit.
	 */
	Map<String, String> getReferrerMapFromUri(Uri uri) {
		MapBuilder paramMap = new MapBuilder();

		// If no URI, return an empty Map.
		if (uri == null) { return paramMap.build(); }

		// Source is the only required campaign field. No need to continue if not present.
		if (uri.getQueryParameter(CAMPAIGN_SOURCE_PARAM) != null) {
			// MapBuilder.setCampaignParamsFromUrl parses Google Analytics campaign ("UTM") parameters from a string URL into a Map that can be set on
			// the Tracker.
			paramMap.setCampaignParamsFromUrl(uri.toString());
		}
		// If no source parameter, set authority to source and medium to "referral".
		else if (uri.getAuthority() != null) {
			paramMap.set(Fields.CAMPAIGN_MEDIUM, "referral");
			paramMap.set(Fields.CAMPAIGN_SOURCE, uri.getAuthority());
		}

		return paramMap.build();
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		getSupportActionBar().setDisplayShowHomeEnabled(true);
		mDrawerToggle.syncState();
	}

	@Override
	public void onResume() {
		super.onResume();
		L.d("");
		AsyncTaskFragment.runTask(this, ACTION_LOAD_ACTIVITY, null);
	}

	private static enum FragmentToDisplay {
		HELP,
		WAIT_FOR_SYNC,
		DEFAULT,
	}

	private FragmentToDisplay getWhichFragmentToDisplay() {
		final List<Account> accounts = new ArrayList<Account>();
		AccountManager mgr = AccountManager.get(MainActivity.this);
		if (mgr == null) {
			return FragmentToDisplay.HELP;
		}
		Account[] availableAccounts = mgr.getAccountsByType(Constants.ACCOUNT_TYPE);
		if (availableAccounts == null || availableAccounts.length <= 0) {
			return FragmentToDisplay.HELP;
		}
		Collections.addAll(accounts, availableAccounts);

		final int nbAccounts = accounts.size();
		final List<Server> serversToRemove, servers;

		if (nbAccounts == 0) {
			return FragmentToDisplay.HELP;
		} else {
			// Compare number of DB servers vs. Account servers
			final ServersDbAdapter db = new ServersDbAdapter(MainActivity.this);
			db.open();
			servers = db.selectAll();
			serversToRemove = new ArrayList<Server>();
			serversToRemove.addAll(servers);
			Account accountToRemove = null;
			for (final Server server : servers) {
				for (final Account account : accounts) {
					if (account.name.equals(server.serverUrl)) {
						serversToRemove.remove(server);
						accountToRemove = account;
						break;
					}
				}

				if (accountToRemove != null) {
					accounts.remove(accountToRemove);
					accountToRemove = null;
				}
			}

			// These are in the DB but not in accounts: they were deleted, so remove everything
			for (final Server server : serversToRemove) {
				L.d("Account " + server + " was deleted, removing all data");
				db.delete(server.rowId);
			}

			final int nbServers = db.getNumServers();
			db.close();

			final ProjectsDbAdapter pdb = new ProjectsDbAdapter(MainActivity.this);
			pdb.open();
			final int nbProjects = pdb.getNumProjects();
			pdb.close();

			if (nbServers > 0 && nbProjects == 0) {
				return FragmentToDisplay.WAIT_FOR_SYNC;
			} else {
				return FragmentToDisplay.DEFAULT;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main_activity, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_main_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		case R.id.menu_main_donate:
			startActivity(new Intent(this, DonateActivity.class));
			return true;

		case R.id.menu_main_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;

		case R.id.menu_main_changelog:
			startActivity(new Intent(this, ChangelogActivity.class));
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onServerProjectPicked(final Server server, final Project project) {
		if (server != null && server.rowId > 0 && project != null && project.id > 0) {
			Intent intent = new Intent(this, EditIssueActivity.class);
			intent.putExtra(Constants.KEY_SERVER, server);
			intent.putExtra(Constants.KEY_PROJECT, project);
			startActivityForResult(intent, IssueUploader.CREATE_ISSUE); // see #onActivityResult below
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		L.d("requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
		if (resultCode == RESULT_OK) {
			final Bundle extras = (data != null && data.getExtras() != null) ? data.getExtras() : new Bundle();
			extras.putInt(IssueUploader.ISSUE_ACTION, requestCode);
			AsyncTaskFragment.runTask(this, ACTION_UPLOAD_ISSUE, extras);
		} else {
			EditIssueActivity.handleRevertCrouton(this, R.id.drawer_content, requestCode, data == null ? null : data.getExtras());
		}
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		switch (action) {
		case ACTION_UPLOAD_ISSUE:
			return IssueUploader.uploadIssue(applicationContext, (Bundle) parameters);

		case ACTION_LOAD_ACTIVITY:
			return getWhichFragmentToDisplay();

		case ACTION_REFRESH_MAIN_SCREEN:
			return WelcomeFragment.buildCards(applicationContext);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);

		switch (action) {
		case ACTION_UPLOAD_ISSUE:
			IssueUploader.handleAddEdit(this, (Bundle) parameters, result);
			break;

		case ACTION_LOAD_ACTIVITY:
			final Fragment contents;
			final Bundle args = new Bundle();

			switch ((FragmentToDisplay) result) {
			default:
			case DEFAULT:
				contents = WelcomeFragment.newInstance(args);
				break;
			case HELP:
				contents = HelpSetupFragment.newInstance(args);
				break;
			case WAIT_FOR_SYNC:
				contents = WaitForSyncFragment.newInstance(args);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						AsyncTaskFragment.runTask(MainActivity.this, ACTION_LOAD_ACTIVITY, null);
					}
				}, 1000 * 30);
				break;
			}

			try {
				getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content, contents).commit();
			} catch (final Exception e) {
				// FATAL EXCEPTION: main
				// java.lang.RuntimeException: Unable to resume activity {net.bicou.redmine/net.bicou.redmine.app.MainActivity}:
				// java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
			}
			break;

		case ACTION_REFRESH_MAIN_SCREEN:
			Fragment frag = getSupportFragmentManager().findFragmentById(R.id.drawer_content);
			if (frag != null && frag instanceof WelcomeFragment) {
				((WelcomeFragment) frag).onCardsBuilt((List<OverviewCard>) result);
			}
			break;
		}
	}
}
