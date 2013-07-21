package net.bicou.redmine.app.wiki;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Created by bicou on 21/07/13.
 */
public class WikiPageActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WikiPageFragment frag = WikiPageFragment.newInstance(getIntent().getExtras());
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, frag).commit();
	}
}
