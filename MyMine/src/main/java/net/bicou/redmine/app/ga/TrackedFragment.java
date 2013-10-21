package net.bicou.redmine.app.ga;

import android.support.v4.app.Fragment;

/**
 * Created by bicou on 07/10/13.
 */
public class TrackedFragment extends Fragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
