package net.bicou.redmine.app.misc;

import android.os.Bundle;

import net.bicou.redmine.app.ActionBarFragmentActivity;

/**
 * Created by bicou on 17/06/13.
 */
public class AboutActivity extends ActionBarFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, new AboutFragment()).commit();
		}
	}
}
