package net.bicou.redmine.app.misc;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AbsMyMineActivity;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.roadmap.RoadmapActivity;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.app.wiki.WikiActivity;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AbsMyMineActivity {
	private static final String ALPHA_SHARED_PREFERENCES_FILE = "alpha";
	private static final String KEY_ALPHA_VERSION_DISCLAIMER = "IS_DISCLAIMER_ACCEPTED";

	public static final String MYMINE_PREFERENCES_FILE = "mymine";
	public static final String KEY_IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";

	DrawerLayout mDrawerLayout;
	ListView mDrawerList;
	private String mTitle, mTitleDrawer;
	ActionBarDrawerToggle mDrawerToggle;

	static class SlidingMenuItem {
		int image, text;

		public SlidingMenuItem(final int image, final int text) {
			this.image = image;
			this.text = text;
		}
	}

	static class SlidingMenuItemViewsHolder {
		ImageView icon;
		TextView text;
	}

	public static final SlidingMenuItem[] mMenu = {
			new SlidingMenuItem(R.drawable.icon_projects, R.string.menu_projects),
			new SlidingMenuItem(R.drawable.icon_issues, R.string.menu_issues),
			new SlidingMenuItem(R.drawable.icon_roadmaps, R.string.menu_roadmap),
			new SlidingMenuItem(R.drawable.icon_wiki, R.string.menu_wiki),
			new SlidingMenuItem(R.drawable.icon_about, R.string.menu_about),
			new SlidingMenuItem(R.drawable.icon_settings, R.string.menu_settings),
	};

	@Override
	public void onPreCreate() {
		prepareIndeterminateProgressActionBar();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDrawerList = (ListView) findViewById(android.R.id.list);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
		mDrawerList.setAdapter(new SlidingMenuItemsAdapter(this, R.layout.slidingmenu_item, R.id.slidingmenu_item_text, mMenu));
		mDrawerList.setOnItemClickListener(mListItemClickListener);

		mTitle = getString(R.string.app_name);
		mTitleDrawer = getString(R.string.drawer_title);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mTitleDrawer);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		showAlphaVersionAlert();
		final boolean isFirstLaunch = getSharedPreferences(MYMINE_PREFERENCES_FILE, 0).getBoolean(KEY_IS_FIRST_LAUNCH, true);

		// Create contents fragment
		if (savedInstanceState == null) {
			// loadFragment();

			if (isFirstLaunch) {
				// No longer the first launch
				final Editor editor = getSharedPreferences(MYMINE_PREFERENCES_FILE, 0).edit();
				editor.putBoolean(KEY_IS_FIRST_LAUNCH, false);
				editor.commit();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		//menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	public static class SlidingMenuItemsAdapter extends ArrayAdapter<SlidingMenuItem> {
		public SlidingMenuItemsAdapter(final Context context, final int resource, final int textViewResourceId, final SlidingMenuItem[] objects) {
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			final SlidingMenuItemViewsHolder h;
			final View v;

			if (convertView == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.slidingmenu_item, null);
				h = new SlidingMenuItemViewsHolder();
				h.icon = (ImageView) v.findViewById(R.id.slidingmenu_item_icon);
				h.text = (TextView) v.findViewById(R.id.slidingmenu_item_text);
				v.setTag(h);
			} else {
				v = convertView;
				h = (SlidingMenuItemViewsHolder) v.getTag();
			}

			final SlidingMenuItem item = getItem(position);
			if (item != null) {
				h.icon.setImageResource(item.image);
				h.text.setText(item.text);
			}

			return v;
		}
	}

	AdapterView.OnItemClickListener mListItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(final AdapterView listView, final View v, final int position, final long id) {
			final Bundle args = new Bundle();
			final Intent intent;
			final AbsMyMineActivity act = MainActivity.this;
			args.putInt(Constants.KEY_PROJECT_POSITION, act.mCurrentProjectPosition);

			switch (mMenu[position].text) {
			case R.string.menu_issues:
				intent = new Intent(act, IssuesActivity.class);
				intent.putExtras(args);
				break;

			case R.string.menu_projects:
				intent = new Intent(act, ProjectsActivity.class);
				intent.putExtras(args);
				break;

			case R.string.menu_roadmap:
				intent = new Intent(act, RoadmapActivity.class);
				intent.putExtras(args);
				break;

			case R.string.menu_wiki:
				intent = new Intent(act, WikiActivity.class);
				intent.putExtras(args);
				break;

			case R.string.menu_about:
				intent = new Intent(act, AboutActivity.class);
				break;

			case R.string.menu_settings:
				intent = new Intent(act, SettingsActivity.class);
				break;

			default:
				intent = new Intent(act, MainActivity.class);
				break;
			}

			startActivity(intent);
			mDrawerLayout.closeDrawer(mDrawerList);
		}
	};


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

	;

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
					getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, contents).commit();
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
	public boolean onOptionsItemSelected(final MenuItem item) {
		final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
		if (f == null) {
			return super.onOptionsItemSelected(item);
		}

		switch (item.getItemId()) {
		// case R.id.menu_refresh:
		// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected boolean shouldDisplayProjectsSpinner() {
		return false;
	}

	@Override
	protected void onCurrentProjectChanged() {
		final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);

		// Already loaded?
		if (f instanceof WelcomeFragment) {
			((WelcomeFragment) f).refreshUI();
		}
		// Likely to be the loading fragment. Change that.
		else {
			final Bundle args = new Bundle();
			getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, WelcomeFragment.newInstance(args)).commit();
		}
	}
}
