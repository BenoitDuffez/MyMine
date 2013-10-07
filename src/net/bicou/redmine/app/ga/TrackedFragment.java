package net.bicou.redmine.app.ga;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Created by bicou on 07/10/13.
 */
public class TrackedFragment extends SherlockFragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
