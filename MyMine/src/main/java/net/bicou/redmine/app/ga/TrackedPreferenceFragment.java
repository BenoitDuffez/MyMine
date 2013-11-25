package net.bicou.redmine.app.ga;

import android.annotation.TargetApi;
import android.os.Build;
import android.preference.PreferenceFragment;

/**
 * Created by bicou on 07/10/13.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TrackedPreferenceFragment extends PreferenceFragment {
	@Override
	public void onResume() {
		super.onResume();
		GAUtils.trackPageView(this);
	}
}
