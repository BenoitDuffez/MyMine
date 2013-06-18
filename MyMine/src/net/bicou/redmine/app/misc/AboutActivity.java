package net.bicou.redmine.app.misc;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Created by bicou on 17/06/13.
 */
public class AboutActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new AboutFragment()).commit();
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
