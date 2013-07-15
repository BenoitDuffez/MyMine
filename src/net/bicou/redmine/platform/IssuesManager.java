package net.bicou.redmine.platform;

import android.content.Context;
import android.content.SyncResult;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.*;
import net.bicou.redmine.data.sqlite.*;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class IssuesManager {
	public static synchronized long updateIssues(final Context context, final Server server, final List<Issue> remoteList, final long lastSyncMarker,
												 final SyncResult syncResult) {
		final IssuesDbAdapter db = new IssuesDbAdapter(context);
		db.open();

		long currentSyncMarker = lastSyncMarker;
		long issueUpdateDate;
		Issue localIssue;
		final Calendar lastKnownChange = new GregorianCalendar();
		lastKnownChange.setTimeInMillis(lastSyncMarker);

		for (final Issue issue : remoteList) {
			localIssue = db.select(server, issue.id, null);
			issue.server = server;

			if (localIssue == null) {
				// New issue, add it
				db.insert(issue);

				if (syncResult != null) {
					syncResult.stats.numInserts++;
					syncResult.stats.numEntries++;
				}
			} else {
				issueUpdateDate = issue.updated_on.getTimeInMillis();

				// Save current sync marker
				if (issueUpdateDate > currentSyncMarker) {
					currentSyncMarker = issueUpdateDate;
				}

				// Check issue status
				if (issue.updated_on.after(localIssue.updated_on)) {
					// Issue is outdated, update it
					db.update(issue);

					if (syncResult != null) {
						syncResult.stats.numUpdates++;
						syncResult.stats.numEntries++;
					}
				} else {
					// Issue is up to date
				}
			}
		}
		// TODO: handle delete?
		db.close();

		return currentSyncMarker;
	}

	public static synchronized long updateIssueStatuses(final Context context, final Server server, final List<IssueStatus> remoteList, final long lastSyncMarker) {
		L.d("ctx=" + context + ", remote issue statuses count=" + remoteList.size() + " syncMarker=" + lastSyncMarker);
		final IssueStatusesDbAdapter db = new IssueStatusesDbAdapter(context);
		db.open();
		db.deleteAll(server);
		for (final IssueStatus issueStatus : remoteList) {
			db.insert(server, issueStatus);
		}
		db.close();

		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updateIssueQueries(final Context ctx, final Server server, final List<Query> remoteQueries, final long lastSyncMarker) {
		final QueriesDbAdapter db = new QueriesDbAdapter(ctx);
		db.open();
		db.deleteAll(server, 0);
		for (final Query query : remoteQueries) {
			query.server = server;
			db.insert(query);
		}
		db.close();

		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updateTrackers(final Context context, final Server server, final List<Tracker> remoteList, final long lastSyncMarker) {
		final TrackersDbAdapter db = new TrackersDbAdapter(context);
		db.open();
		db.deleteAll(server);
		for (final Tracker tracker : remoteList) {
			tracker.server = server;
			db.insert(server, tracker);
		}
		db.close();

		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updatePriorities(final Context context, final Server server, final List<IssuePriority> remoteList, final long lastSyncMarker) {
		final IssuePrioritiesDbAdapter db = new IssuePrioritiesDbAdapter(context);
		db.open();
		db.deleteAll(server);
		for (final IssuePriority priority : remoteList) {
			priority.server = server;
			db.insert(priority);
		}
		db.close();

		return new GregorianCalendar().getTimeInMillis();
	}
}
