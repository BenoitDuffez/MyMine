package net.bicou.redmine.app.ga;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Created by bicou on 07/10/13.
 */
public class TrackedDialogFragment extends SherlockDialogFragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
