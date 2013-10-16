package net.bicou.redmine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.SyncStats;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.app.ssl.SupportSSLKeyManager;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssuePrioritiesList;
import net.bicou.redmine.data.json.IssueStatusesList;
import net.bicou.redmine.data.json.IssuesList;
import net.bicou.redmine.data.json.TrackersList;
import net.bicou.redmine.data.sqlite.IssuePrioritiesDbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesList;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.TrackersDbAdapter;
import net.bicou.redmine.platform.IssuesManager;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.PreferencesManager;

/**
 * Service to handle Account sync. This is invoked with an intent with action ACTION_AUTHENTICATOR_INTENT. It instantiates the syncadapter and returns its IBinder.
 */
public class IssuesSyncAdapterService extends Service {
	public static final String SYNC_MARKER_KEY = "net.bicou.redmine.sync.Issues.marker";
	private static final Object sSyncAdapterLock = new Object();

	private static SyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		L.d("");
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(final Intent intent) {
		L.d("");
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
			L.d("context=" + context + " autoinit=" + autoInitialize);
			mContext = context;
			mAccountManager = AccountManager.get(context);
		}

		@Override
		public void onPerformSync(final Account account, final Bundle extras, final String authority, final ContentProviderClient provider,
								  final SyncResult syncResult) {
			// see if we already have a sync-state attached to this account. By handing this value to the server,
			// we can just get the contacts that have been updated on the server-side since our last sync-up
			final long lastSyncMarker = getServerSyncMarker(account);

			// Init SSL and certificates
			SupportSSLKeyManager.init(mContext);

			final ServersDbAdapter db = new ServersDbAdapter(mContext);
			db.open();
			Server server = db.getServer(account.name);
			db.close();

			if (server == null) {
				L.e("Couldn't get the server for account: " + account, null);
				return;
			}

			Synchronizer sync = new Synchronizer(mContext);
			long newSyncState = sync.synchronizeIssues(server, syncResult, lastSyncMarker);


			// Save off the new sync marker. On our next sync, we only
			// want to receive
			// contacts that have changed since this sync...
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

	public static class Synchronizer {
		private final AccountManager mAccountManager;
		private final Context mContext;

		public Synchronizer(Context context) {
			mContext = context;
			mAccountManager = AccountManager.get(context);
		}

		public long synchronizeIssues(final Server server, final SyncResult syncResult, long lastSyncMarker) {
			int offset = 0;
			IssuesList issues;
			long newSyncState = 0;
			SyncResult before = null;
			if (syncResult != null) {
				final Parcel parcel = Parcel.obtain();
				syncResult.writeToParcel(parcel, 0);
				before = SyncResult.CREATOR.createFromParcel(parcel);
				parcel.recycle();
			}

			// Get the issues sync period preference
			final int issuesSyncPeriod = PreferencesManager.getInt(mContext, SettingsActivity.KEY_ISSUES_SYNC_PERIOD, 182);
			IssuesDbAdapter db = new IssuesDbAdapter(mContext);
			db.open();

			do {
				issues = NetworkUtilities.syncIssues(mContext, server, issuesSyncPeriod, lastSyncMarker, offset);
				if (issues == null || issues.issues == null || issues.issues.size() <= 0) {
					break;
				}
				newSyncState = IssuesManager.updateIssues(db, server, issues.issues, lastSyncMarker, syncResult);
				offset += issues.downloadedObjects;
			} while (offset < issues.total_count);

			if (before != null && (before.stats.numInserts < syncResult.stats.numInserts || before.stats.numUpdates < syncResult.stats.numUpdates)) {
				// displayIssuesNotification(mContext, before.stats,
				// syncResult.stats);
			}

			// Sync issue statuses as well
			final IssueStatusesList issuesStatuses = NetworkUtilities.syncIssueStatuses(mContext, server, lastSyncMarker);
			if (issuesStatuses != null && issuesStatuses.issue_statuses != null && issuesStatuses.issue_statuses.size() > 0) {
				IssuesManager.updateIssueStatuses(new IssueStatusesDbAdapter(db), server, issuesStatuses.issue_statuses, lastSyncMarker);
			}

			// Sync issue queries
			final QueriesList queries = NetworkUtilities.syncQueriesList(mContext, server, lastSyncMarker);
			if (queries != null && queries.queries != null && queries.queries.size() > 0) {
				IssuesManager.updateIssueQueries(new QueriesDbAdapter(db), server, queries.queries, lastSyncMarker);
			}

			// Sync issue trackers
			final TrackersList trackers = NetworkUtilities.syncTrackers(mContext, server, lastSyncMarker);
			if (trackers != null && trackers.trackers != null && trackers.trackers.size() > 0) {
				IssuesManager.updateTrackers(new TrackersDbAdapter(db), server, trackers.trackers, lastSyncMarker);
			}

			// Sync issue priorities
			IssuePrioritiesList priorities = NetworkUtilities.syncIssuePriorities(mContext, server, lastSyncMarker);
			if (priorities != null && priorities.issue_priorities != null && priorities.issue_priorities.size() > 0) {
				IssuesManager.updatePriorities(new IssuePrioritiesDbAdapter(db), server, priorities.issue_priorities, lastSyncMarker);
			}

			db.close();
			return newSyncState;
		}

		private void displayIssuesNotification(final Context ctx, final SyncStats before, final SyncStats after) {
			final String notif;
			if (before.numInserts < after.numInserts && before.numUpdates < after.numUpdates) {
				notif = String.format("%d issues were added, and %d were modified", after.numInserts - before.numInserts, after.numUpdates - before.numUpdates);
			} else if (before.numInserts < after.numInserts) {
				notif = String.format("%d issues were added", after.numInserts - before.numInserts);
			} else {
				notif = String.format("%d issues were modified", after.numUpdates - before.numUpdates);
			}

			final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx).setSmallIcon(R.drawable.icon_issues).setContentTitle("MyMine " +
					"issues " +
					"update");
			mBuilder.setContentText(notif);

			// Creates an explicit intent for an Activity in your app
			final Intent resultIntent = new Intent(ctx, IssuesActivity.class);
			final Bundle args = new Bundle();
			args.putInt(Constants.KEY_PROJECT_POSITION, 0);
			resultIntent.putExtras(args);

			// The stack builder object will contain an artificial back stack
			// for the started Activity.
			// This ensures that navigating backward from the Activity leads out
			// of your application to the Home screen.
			final TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(IssuesActivity.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);

			final PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			final NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(0, mBuilder.build());
		}
	}
}
