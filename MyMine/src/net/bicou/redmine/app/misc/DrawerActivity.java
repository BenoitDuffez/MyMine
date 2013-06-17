package net.bicou.redmine.app.misc;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.redmine.R;
import net.bicou.redmine.app.drawers.DrawerMenuFragment;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 12/06/13.
 */
public class DrawerActivity extends SherlockFragmentActivity {
	DrawerLayout mDrawerLayout;
	View mDrawerMenu;
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
	 * @return
	 */
	protected Fragment getDrawerFragment() {
		return new DrawerMenuFragment();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_drawer);

		mDrawerMenu = findViewById(R.id.navigation_drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.content, getDrawerFragment()).commit();
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
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mTitleDrawer);
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

	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerMenu);

		MenuItem menuItem = menu.findItem(R.id.menu_drawer_customize);
		if (menuItem == null) {
			if (drawerOpen) {
				getSupportMenuInflater().inflate(R.menu.menu_navigation_drawer, menu);
			}
		} else {
			menuItem.setVisible(drawerOpen);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		L.d("item: " + item);
		switch (item.getItemId()) {
		case android.R.id.home:
			if (!this.getClass().equals(MainActivity.class)) {
				NavUtils.navigateUpFromSameTask(this);
				return true;
			} else {
				if (mDrawerLayout.isDrawerOpen(mDrawerMenu)) {
					mDrawerLayout.closeDrawer(mDrawerMenu);
				} else {
					mDrawerLayout.openDrawer(mDrawerMenu);
				}
			}
		}
		return super.onOptionsItemSelected(item);
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
}
