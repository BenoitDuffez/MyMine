package net.bicou.redmine.app.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedPreferenceFragment;

/**
 * This fragment shows notification preferences only. It is used when the activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NotificationPreferenceFragment extends TrackedPreferenceFragment {
	public NotificationPreferenceFragment() {
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_notification);

		// Bind the summaries of EditText/List/Dialog/Ringtone preferences
		// to their values. When their values change, their summaries are
		// updated to reflect the new value, per the Android Design
		// guidelines.
		SettingsActivity.bindPreferenceSummaryToValue(findPreference(SettingsActivity.KEY_NOTIFICATION_RINGTONE));
	}
}
