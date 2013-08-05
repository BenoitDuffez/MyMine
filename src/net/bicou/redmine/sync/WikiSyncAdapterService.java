package net.bicou.redmine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import net.bicou.redmine.app.ssl.SupportSSLKeyManager;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPagesIndex;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.platform.WikiManager;
import net.bicou.redmine.util.L;

import java.util.List;

/**
 * Service to handle Account sync. This is invoked with an intent with action ACTION_AUTHENTICATOR_INTENT. It instantiates the syncadapter and returns its IBinder.
 */
public class WikiSyncAdapterService extends Service {
	public static final String SYNC_MARKER_KEY = "net.bicou.redmine.sync.Wiki.marker";
	private static final Object sSyncAdapterLock = new Object();

	private static SyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}

	/**
	 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the platform ContactOperations provider. This sample shows a basic 2-way sync between
	 * the
	 * client and a sample server. It also contains an example of how to update the contacts' status messages, which would be useful for a messaging or social
	 * networking client.
	 */
	private static class SyncAdapter extends AbstractThreadedSyncAdapter {
		private static final boolean NOTIFY_AUTH_FAILURE = true;

		private final AccountManager mAccountManager;

		private final Context mContext;

		public SyncAdapter(final Context context, final boolean autoInitialize) {
			super(context, autoInitialize);
			mContext = context;
			mAccountManager = AccountManager.get(context);
		}

		@Override
		public void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider,
								  final SyncResult syncResult) {
			L.d("account=" + account + " extras=" + extras + " auth=" + authority + " prov=" + provider + " result=" + syncResult);

			final long lastSyncMarker = getServerSyncMarker(account);

			final ServersDbAdapter db = new ServersDbAdapter(mContext);
			db.open();
			Server server = db.getServer(account.name);
			db.close();

			if (server == null) {
				L.e("Couldn't get the server", null);
				return;
			}

			// Init SSL and certificates
			SupportSSLKeyManager.init(mContext);

			// Get all projects
			final ProjectsDbAdapter pdb = new ProjectsDbAdapter(mContext);
			pdb.open();
			final List<Project> projects = pdb.selectAll();
			pdb.close();

			// Sync all wiki pages of all projects
			long newSyncState = 0, tmp;
			for (final Project project : projects) {
				final WikiPagesIndex pages = NetworkUtilities.syncWiki(mContext, server, project, lastSyncMarker);
				if (pages != null && pages.wiki_pages != null && pages.wiki_pages.size() > 0) {
					tmp = WikiManager.updateWiki(mContext, account, server, project, pages.wiki_pages, lastSyncMarker);
					if (tmp > newSyncState) {
						newSyncState = tmp;
					}
				}
			}

			if (newSyncState > 0) {
				setServerSyncMarker(account, newSyncState);
			}
		}

		/**
		 * This helper function fetches the last known high-water-mark we received from the server - or 0 if we've never synced.
		 *
		 * @param account the account we're syncing
		 *
		 * @return the change high-water-mark
		 */
		private long getServerSyncMarker(final Account account) {
			final String markerString = mAccountManager.getUserData(account, SYNC_MARKER_KEY);
			if (!TextUtils.isEmpty(markerString)) {
				return Long.parseLong(markerString);
			}
			return 0;
		}

		/**
		 * Save off the high-water-mark we receive back from the server.
		 *
		 * @param account The account we're syncing
		 * @param marker  The high-water-mark we want to save.
		 */
		private void setServerSyncMarker(final Account account, final long marker) {
			mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
		}
	}
}
