package net.bicou.redmine.app.drawers;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;
import net.bicou.redmine.app.drawers.main.DrawerMenuFragment;
import net.bicou.redmine.app.misc.MainActivity;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 12/06/13.
 */
public abstract class DrawerActivity extends ActionBarActivity {
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

    int setContentViewCount = 0;

    @Override
    public void setContentView(int layoutResId) {
        setContentViewCount++;
        if (setContentViewCount == 1) {
            super.setContentView(layoutResId);
        } else {
            throw new IllegalStateException("Don't call setContentView from your activity");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_drawer);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(false);


        mDrawerMenu = findViewById(R.id.navigation_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);

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
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mTitleDrawer);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                Fragment frag = getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
                if (frag instanceof DrawerMenuFragment) {
                    ((DrawerMenuFragment) frag).refreshMenu();
                }
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

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }
}
