package net.bicou.redmine.sync;

import android.content.Context;
import net.bicou.redmine.Constants;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.*;
import net.bicou.redmine.data.sqlite.QueriesList;
import net.bicou.redmine.net.JsonDownloader;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Provides utility methods for communicating with the server.
 */
final public class NetworkUtilities {
	/**
	 * Try to get info about the local user
	 */
	public static User whoAmI(final Context ctx, final Server server) {
		return new JsonDownloader<User>(User.class) //
				.setStripJsonContainer(true) //
				.fetchObject(ctx, server, "users/current.json");
	}

	public static ProjectsList syncProjects(final Context ctx, final Server server, final long serverSyncState) {
		final ProjectsList list = new JsonDownloader<ProjectsList>(ProjectsList.class).fetchObject(ctx, server, "projects.json");
		return list;
	}

	public static IssuesList syncIssues(final Context ctx, final Server server, final int syncPeriod, final long serverSyncState, final int startOffset) {
		final List<NameValuePair> args = new ArrayList<NameValuePair>();

		args.add(new BasicNameValuePair("limit", Integer.toString(Constants.ISSUES_LIST_BURST_DOWNLOAD_COUNT)));
		args.add(new BasicNameValuePair("status_id", "*"));
		args.add(new BasicNameValuePair("offset", Integer.toString(startOffset)));
		args.add(new BasicNameValuePair("include", "attachments"));

		// Create updated_on filter
		if (syncPeriod > 0) {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			final GregorianCalendar cal = new GregorianCalendar();
			String updatedOnFilter = sdf.format(cal.getTime());
			if (serverSyncState <= 0) {
				cal.add(Calendar.DATE, -1 * syncPeriod);
			} else {
				cal.setTimeInMillis(serverSyncState);
			}
			updatedOnFilter = "><" + sdf.format(cal.getTime()) + "|" + updatedOnFilter;
			args.add(new BasicNameValuePair("updated_on", updatedOnFilter));
		}

		// Download issues
		return new JsonDownloader<IssuesList>(IssuesList.class).setDownloadAllIfList(false).fetchObject(ctx, server, "issues.json", args);
	}

	public static IssueStatusesList syncIssueStatuses(final Context ctx, final Server server, final long serverSyncState) {
		return new JsonDownloader<IssueStatusesList>(IssueStatusesList.class).fetchObject(ctx, server, "issue_statuses.json");
	}

	public static VersionsList syncVersions(final Context ctx, final Server server, final long projectId, final long serverSyncState) {
		final String url = String.format(Locale.ENGLISH, "projects/%d/versions.json", projectId);
		return new JsonDownloader<VersionsList>(VersionsList.class).fetchObject(ctx, server, url);
	}

	public static WikiPagesIndex syncWiki(final Context ctx, final Server server, final Project project, final long serverSyncState) {
		final String url = String.format(Locale.ENGLISH, "projects/%d/wiki/index.json", project.id);
		return new JsonDownloader<WikiPagesIndex>(WikiPagesIndex.class).fetchObject(ctx, server, url);
	}

	public static QueriesList syncQueriesList(final Context ctx, final Server server, final long serverSyncState) {
		return new JsonDownloader<QueriesList>(QueriesList.class).fetchObject(ctx, server, "queries.json");
	}

	public static UsersList syncUsers(final Context ctx, final Server server, final long serverSyncState) {
		return new JsonDownloader<UsersList>(UsersList.class).fetchObject(ctx, server, "users.json");
	}

	public static TrackersList syncTrackers(Context ctx, Server server, long serverSyncState) {
		return new JsonDownloader<TrackersList>(TrackersList.class).setDownloadAllIfList(true).fetchObject(ctx, server, "trackers.json");
	}

	public static IssueCategoriesList syncIssueCategories(Context ctx, Server server, Project project, long serverSyncState) {
		String url = "projects/" + project.id + "/issue_categories.json";
		return new JsonDownloader<IssueCategoriesList>(IssueCategoriesList.class).setDownloadAllIfList(true).fetchObject(ctx, server, url);
	}

	public static IssuePrioritiesList syncIssuePriorities(Context ctx, Server server, long serverSyncState) {
		String url = "enumerations/issue_priorities.json";
		return new JsonDownloader<IssuePrioritiesList>(IssuePrioritiesList.class).setDownloadAllIfList(true).fetchObject(ctx, server, url);
	}
}
