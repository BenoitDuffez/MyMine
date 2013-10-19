package net.bicou.redmine.app.ga;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Created by bicou on 07/10/13.
 */
public class TrackedListFragment extends SherlockListFragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
