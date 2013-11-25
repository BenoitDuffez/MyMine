package net.bicou.redmine.app.ga;

import android.support.v4.app.ListFragment;

/**
 * Created by bicou on 07/10/13.
 */
public class TrackedListFragment extends ListFragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
