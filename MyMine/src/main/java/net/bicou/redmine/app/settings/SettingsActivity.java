package net.bicou.redmine.app.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.sync.IssuesSyncAdapterService;
import net.bicou.redmine.sync.ProjectsSyncAdapterService;
import net.bicou.redmine.sync.SyncUtils;
import net.bicou.redmine.sync.WikiSyncAdapterService;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of the list of settings.
 */
public class SettingsActivity extends PreferenceActivity {
	public static final String KEY_SYNC_FREQUENCY = "sync_frequency";
	public static final String KEY_ISSUES_SYNC_PERIOD = "issues_sync_period";
	public static final String KEY_NOTIFICATION_RINGTONE = "notifications_new_message_ringtone";

	/**
	 * Determines whether to always show the simplified settings UI, where settings are presented in a single list. When false, settings are shown as
	 * a master/detail two-pane view on tablets. When true, a single pane is shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
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

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
			// NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(final Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setupSimplePreferencesScreen();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);

		// Add 'notifications' preferences, and a corresponding header.
		PreferenceCategory fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_notifications);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_notification);

		// Add 'data and sync' preferences, and a corresponding header.
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_data_sync);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_data_sync);

		// Bind the summaries of EditText/List/Dialog/Ringtone preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.
		bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATION_RINGTONE));
		bindPreferenceSummaryToValue(findPreference(KEY_SYNC_FREQUENCY));
		bindPreferenceSummaryToValue(findPreference(KEY_ISSUES_SYNC_PERIOD));
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(final Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			return isXLargeTablet_api9(context);
		}
		return false;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private static boolean isXLargeTablet_api9(final Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(final Context context) {
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(final List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(final Preference preference, final Object value) {
			final String stringValue = value.toString();

			// Was this preference just changed by the user?
			if (!TextUtils.isEmpty(preference.getSummary())) {
				// Reset the sync service scheduling
				if (KEY_SYNC_FREQUENCY.equals(preference.getKey())) {
					SyncUtils.updateSyncPeriod(preference.getContext());
				}

				// Reset sync markers
				else if (KEY_ISSUES_SYNC_PERIOD.equals(preference.getKey())) {
					final AccountManager am = (AccountManager) preference.getContext().getSystemService(Context.ACCOUNT_SERVICE);
					final Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
					for (final Account account : accounts) {
						am.setUserData(account, IssuesSyncAdapterService.SYNC_MARKER_KEY, "0");
						am.setUserData(account, ProjectsSyncAdapterService.SYNC_MARKER_KEY, "0");
						am.setUserData(account, WikiSyncAdapterService.SYNC_MARKER_KEY, "0");
					}
				}
			}

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in the preference's 'entries' list.
				final ListPreference listPreference = (ListPreference) preference;
				final int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			}

			// For ringtone preferences, look up the correct display value using RingtoneManager.
			else if (preference instanceof RingtonePreference) {
				// Empty values correspond to 'silent' (no ringtone).
				if (TextUtils.isEmpty(stringValue)) {
					preference.setSummary(R.string.pref_ringtone_silent);
				} else {
					final Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

					// Clear the summary if there was a lookup error.
					if (ringtone == null) {
						preference.setSummary(null);
					}
					// Set the summary to reflect the new ringtone display name.
					else {
						final String name = ringtone.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}
			}

			// For all other preferences, set the summary to the value's simple string representation.
			else {
				preference.setSummary(stringValue);
			}

			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also immediately updated upon calling this method. The exact display format
	 * is dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	static void bindPreferenceSummaryToValue(final Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext())
				.getString(preference.getKey(), ""));
	}
}
