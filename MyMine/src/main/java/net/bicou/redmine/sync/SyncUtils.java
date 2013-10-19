package net.bicou.redmine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.util.PreferencesManager;

public class SyncUtils {
	public static final String[] SYNC_AUTHORITIES = {
			"net.bicou.redmine.sync.Projects",
			"net.bicou.redmine.sync.Issues",
			"net.bicou.redmine.sync.Wiki",
			"net.bicou.redmine.sync.Users",
	};

	/**
	 * Enable sync on all authorities and all accounts
	 */
	public static void enableSync(final Context ctx) {
		try {
			final AccountManager mgr = AccountManager.get(ctx);
			for (final Account account : mgr.getAccounts()) {
				enableSync(account, ctx);
			}
		} catch (final Exception e) {
		}
	}

	/**
	 * Enable sync on all authorities for a given account
	 */
	public static void enableSync(final Account account, final Context ctx) {
		for (final String authority : SYNC_AUTHORITIES) {
			enableSync(account, ctx, authority);
		}
	}

	/**
	 * Update only the sync period on all accounts and authorities
	 */
	public static void updateSyncPeriod(final Context ctx) {
		try {
			final AccountManager mgr = AccountManager.get(ctx);
			for (final Account account : mgr.getAccounts()) {
				updateSyncPeriod(account, ctx);
			}
		} catch (final Exception e) {
		}
	}

	/**
	 * Update only the sync period on all authorities for a given account
	 */
	public static void updateSyncPeriod(final Account account, final Context ctx) {
		for (final String authority : SYNC_AUTHORITIES) {
			updateSyncPeriod(account, ctx, authority);
		}
	}

	/**
	 * Update only the sync period for a given account and authority
	 */
	public static void updateSyncPeriod(final Account account, final Context ctx, final String authority) {
		ContentResolver.addPeriodicSync(account, authority, new Bundle(), getSyncInterval(ctx));
	}

	/**
	 * Return the sync interval from the user's preferences
	 *
	 * @return the sync interval, in seconds
	 */
	public static long getSyncInterval(final Context ctx) {
		return 60L * PreferencesManager.getLong(ctx, SettingsActivity.KEY_SYNC_FREQUENCY, 1400);
	}

	/**
	 * Enable sync for a given account and authority
	 */
	public static void enableSync(final Account account, final Context ctx, final String authority) {
		ContentResolver.setIsSyncable(account, authority, 1);
		ContentResolver.setSyncAutomatically(account, authority, true);
		ContentResolver.addPeriodicSync(account, authority, new Bundle(), getSyncInterval(ctx));
	}
}
