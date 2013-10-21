package net.bicou.redmine.app.wiki;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import com.google.analytics.tracking.android.EasyTracker;
import net.bicou.redmine.app.AsyncTaskFragment;

/**
 * Created by bicou on 21/07/13.
 */
public class WikiPageActivity extends ActionBarActivity implements AsyncTaskFragment.TaskFragmentCallbacks {
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AsyncTaskFragment.attachAsyncTaskFragment(this);
		WikiPageFragment frag = WikiPageFragment.newInstance(getIntent().getExtras());
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, frag).commit();
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
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		if (action == WikiPageFragment.ACTION_LOAD_WIKI_PAGE) {
			return WikiPageFragment.loadWikiPage(applicationContext, (WikiPageFragment.WikiPageLoadParameters) parameters);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		if (action == WikiPageFragment.ACTION_LOAD_WIKI_PAGE) {
			Fragment frag = getSupportFragmentManager().findFragmentById(android.R.id.content);
			if (frag != null && frag instanceof WikiPageFragment) {
				((WikiPageFragment) frag).refreshUI((WikiPageFragment.WikiPageLoadParameters) parameters);
			}
		}
	}
}
