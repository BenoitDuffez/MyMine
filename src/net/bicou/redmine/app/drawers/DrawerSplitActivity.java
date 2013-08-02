package net.bicou.redmine.app.drawers;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import net.bicou.android.splitscreen.SplitActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.app.misc.MainActivity;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 12/06/13.
 */
public abstract class DrawerSplitActivity<MainFragment extends Fragment, ContentFragment extends Fragment> extends SplitActivity<MainFragment, ContentFragment> {
	DrawerLayout mDrawerLayout;
	View mDrawerMenu;
	private String mTitle, mTitleDrawer;
	protected ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		L.d("newConfig: " + newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Can be overriden by subclasses
	 */
	protected Fragment getDrawerFragment() {
		return new DrawerMenuFragment();
	}

	boolean canSetContentView = false;

	@Override
	public void setContentView(int layoutResId) {
		L.d("");
		if (canSetContentView) {
			super.setContentView(R.layout.activity_drawer_split);
			canSetContentView = false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);

		super.onCreate(savedInstanceState);
		canSetContentView = true;
		setContentView(0);

		mDrawerMenu = findViewById(R.id.navigation_drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.navigation_drawer, getDrawerFragment()).commit();
		}

		if (mDrawerMenu == null || mDrawerLayout == null) {
			throw new IllegalStateException("a DrawerActivity must have a drawer layout id=main_drawer_layout and a drawer list id=android.R.id.list");
		}

		mTitle = getString(R.string.app_name);
		mTitleDrawer = getString(R.string.drawer_title);
	}

	public void closeDrawer() {
		mDrawerLayout.closeDrawer(mDrawerMenu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				getSupportActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getSupportActionBar().setTitle(mTitleDrawer);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		ActionBar actionBar = getSupportActionBar();

		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(!getClass().equals(MainActivity.class));
		//actionBar.setDisplayUseLogoEnabled(!getClass().equals(MainActivity.class));

		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(mDrawerMenu)) {
				mDrawerLayout.closeDrawer(mDrawerMenu);
			} else {
				mDrawerLayout.openDrawer(mDrawerMenu);
			}
		}
		return super.onOptionsItemSelected(item);
	}
}
