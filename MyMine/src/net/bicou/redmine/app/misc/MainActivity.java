package net.bicou.redmine.app.misc;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.drawers.DrawerActivity;
import net.bicou.redmine.app.drawers.DrawerMenuFragment;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends DrawerActivity {
	private static final String ALPHA_SHARED_PREFERENCES_FILE = "alpha";
	private static final String KEY_ALPHA_VERSION_DISCLAIMER = "IS_DISCLAIMER_ACCEPTED";

	public static final String MYMINE_PREFERENCES_FILE = "mymine";
	public static final String KEY_IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showAlphaVersionAlert();
		final boolean isFirstLaunch = getSharedPreferences(MYMINE_PREFERENCES_FILE, 0).getBoolean(KEY_IS_FIRST_LAUNCH, true);

		if (savedInstanceState == null && isFirstLaunch) {
			// No longer the first launch
			final Editor editor = getSharedPreferences(MYMINE_PREFERENCES_FILE, 0).edit();
			editor.putBoolean(KEY_IS_FIRST_LAUNCH, false);
			editor.commit();
		}

		getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content, LoadingFragment.newInstance()).commit();
		getSupportFragmentManager().beginTransaction().replace(R.id.navigation_drawer, new DrawerMenuFragment()).commit();
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
		refreshContents();
	}

	private static enum FragmentToDisplay {
		HELP,
		WAIT_FOR_SYNC,
		DEFAULT,
	}

	private void refreshContents() {
		new AsyncTask<Void, Void, FragmentToDisplay>() {
			@Override
			protected void onPreExecute() {
				setSupportProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected FragmentToDisplay doInBackground(final Void... params) {
				final List<Account> accounts = new ArrayList<Account>();
				for (final Account a : AccountManager.get(MainActivity.this).getAccountsByType(Constants.ACCOUNT_TYPE)) {
					accounts.add(a);
				}
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
			protected void onPostExecute(final FragmentToDisplay result) {
				final Fragment contents;
				final Bundle args = new Bundle();

				switch (result) {
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
							refreshContents();
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

				setSupportProgressBarIndeterminateVisibility(false);
			}
		}.execute();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_main_activity, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final Fragment f = getSupportFragmentManager().findFragmentById(R.id.drawer_content);
		if (f == null) {
			return super.onOptionsItemSelected(item);
		}

		switch (item.getItemId()) {
		case R.id.menu_main_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		case R.id.menu_main_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;

		// case R.id.menu_refresh:
		// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
