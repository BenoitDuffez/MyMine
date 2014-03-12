package net.bicou.redmine.platform;

import android.content.SyncResult;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssuePriority;
import net.bicou.redmine.data.json.IssueStatus;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.sqlite.IssuePrioritiesDbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class IssuesManager {
	public static synchronized long updateIssues(final IssuesDbAdapter db, final Server server, final List<Issue> remoteList, final long lastSyncMarker,
	                                             final SyncResult syncResult) {
		long currentSyncMarker = lastSyncMarker;
		long issueUpdateDate;
		Issue localIssue;
		final Calendar lastKnownChange = new GregorianCalendar();
		lastKnownChange.setTimeInMillis(lastSyncMarker);
		ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);

		for (final Issue issue : remoteList) {
			localIssue = db.select(server, issue.id, null);
			issue.server = server;

			if (pdb.isSyncBlocked(server.rowId, issue.project.id)) {
				continue;
			}

			if (localIssue == null) {
				// New issue, add it
				db.insert(issue);

				if (syncResult != null) {
					syncResult.stats.numInserts++;
					syncResult.stats.numEntries++;
				}
			} else {
				issueUpdateDate = issue.updated_on.getTimeInMillis();

				// Update non-redmine statuses
				issue.is_favorite = localIssue.is_favorite;

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

		return currentSyncMarker;
	}

	public static synchronized long updateIssueStatuses(final IssueStatusesDbAdapter db, final Server server, final List<IssueStatus> remoteList) {
		L.d("remote issue statuses count=" + remoteList.size());
		db.deleteAll(server);
		for (final IssueStatus issueStatus : remoteList) {
			db.insert(server, issueStatus);
		}

		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updateIssueQueries(final QueriesDbAdapter db, final Server server, final List<Query> remoteQueries) {
		db.deleteAll(server, 0);
		for (final Query query : remoteQueries) {
			query.server = server;
			db.insert(query);
		}

		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updatePriorities(final IssuePrioritiesDbAdapter db, final Server server, final List<IssuePriority> remoteList) {
		db.deleteAll(server);
		for (final IssuePriority priority : remoteList) {
			priority.server = server;
			db.insert(priority);
		}

		return new GregorianCalendar().getTimeInMillis();
	}
}
