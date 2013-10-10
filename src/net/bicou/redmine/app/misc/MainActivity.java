package net.bicou.redmine.app.misc;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.drawers.DrawerActivity;
import net.bicou.redmine.app.drawers.main.DrawerMenuFragment;
import net.bicou.redmine.app.issues.edit.EditIssueActivity;
import net.bicou.redmine.app.issues.edit.IssueUploader;
import net.bicou.redmine.app.issues.edit.ServerProjectPickerFragment;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.app.welcome.WelcomeFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends DrawerActivity implements ServerProjectPickerFragment.ServerProjectSelectionListener, AsyncTaskFragment.TaskFragmentCallbacks {
	private static final String ALPHA_SHARED_PREFERENCES_FILE = "alpha";
	private static final String KEY_ALPHA_VERSION_DISCLAIMER = "IS_DISCLAIMER_ACCEPTED";
	private static final String KEY_UPDATE_TO_010_WIKI_PAGES = "IS_010_UPGRADED";

	public static final String MYMINE_PREFERENCES_FILE = "mymine";
	public static final String KEY_IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";

	private static final int ACTION_LOAD_ACTIVITY = 0;
	private static final int ACTION_UPLOAD_ISSUE = 1;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);

		super.onCreate(savedInstanceState);

		showAlphaVersionAlert();
		show010UpgradeAlert();
		final boolean isFirstLaunch = getSharedPreferences(MYMINE_PREFERENCES_FILE, 0).getBoolean(KEY_IS_FIRST_LAUNCH, true);

		if (savedInstanceState == null && isFirstLaunch) {
			// No longer the first launch
			final Editor editor = getSharedPreferences(MYMINE_PREFERENCES_FILE, 0).edit();
			editor.putBoolean(KEY_IS_FIRST_LAUNCH, false);
			editor.commit();
		}

		getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content, LoadingFragment.newInstance()).commit();
		getSupportFragmentManager().beginTransaction().replace(R.id.navigation_drawer, new DrawerMenuFragment()).commit();

		AsyncTaskFragment.attachAsyncTaskFragment(this);
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

	private void showAlphaVersionAlert() {
		final boolean isAccepted = getSharedPreferences(ALPHA_SHARED_PREFERENCES_FILE, 0).getBoolean(KEY_ALPHA_VERSION_DISCLAIMER, false);

		if (!isAccepted) {
			// 1. Instantiate an AlertDialog.Builder with its constructor
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);

			// 2. Chain together various setter methods to set the dialog characteristics
			builder.setMessage(R.string.alert_alpha).setTitle(R.string.alert_alpha_title);
			builder.setPositiveButton(R.string.alpha_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					final Editor editor = getSharedPreferences(ALPHA_SHARED_PREFERENCES_FILE, 0).edit();
					editor.putBoolean(KEY_ALPHA_VERSION_DISCLAIMER, true);
					editor.commit();
				}
			});
			builder.setNegativeButton(R.string.alpha_ko, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
				}
			});

			// 3. Get the AlertDialog from create()
			final AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private void show010UpgradeAlert() {
		final boolean isAccepted = getSharedPreferences(ALPHA_SHARED_PREFERENCES_FILE, 0).getBoolean(KEY_UPDATE_TO_010_WIKI_PAGES, false);

		if (!isAccepted) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.alert_010_upgrade).setTitle(R.string.alert_010_upgrade_title);
			builder.setPositiveButton(R.string.upgrade_010_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					final Editor editor = getSharedPreferences(ALPHA_SHARED_PREFERENCES_FILE, 0).edit();
					editor.putBoolean(KEY_UPDATE_TO_010_WIKI_PAGES, true);
					editor.commit();
				}
			});

			final AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_main_activity, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_main_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		case R.id.menu_main_about:
			startActivity(new Intent(this, AboutActivity.class));
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
			final Bundle extras = data == null || data.getExtras() == null ? new Bundle() : data.getExtras();
			extras.putInt(IssueUploader.ISSUE_ACTION, requestCode);
			AsyncTaskFragment.runTask(this, ACTION_UPLOAD_ISSUE, extras);
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
		}
		return null;
	}

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
		}
	}
}
