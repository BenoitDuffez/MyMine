package net.bicou.redmine.app.ga;

import android.support.v4.app.DialogFragment;

/**
 * Created by bicou on 07/10/13.
 */
public class TrackedDialogFragment extends DialogFragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
