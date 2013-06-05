package net.bicou.redmine.util;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PreferencesManager {
	public static long getLong(final Context ctx, final String key, final long defaultValue) {
		try {
			return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, Long.toString(defaultValue)));
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	public static int getInt(final Context ctx, final String key, final int defaultValue) {
		try {
			return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, Integer.toString(defaultValue)));
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	public static boolean setString(final Context ctx, final String key, final String value) {
		final Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
		editor.putString(key, value);
		return editor.commit();
	}

	public static String getString(final Context ctx, final String key, final String defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, defaultValue);
	}
}
