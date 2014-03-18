package net.bicou.redmine.app.misc;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;

/**
 * Created by bicou on 17/06/13.
 */
public class DonateActivity extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.about_content, new AboutFragment()).commit();
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
