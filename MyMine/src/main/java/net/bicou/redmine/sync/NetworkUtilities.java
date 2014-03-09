package net.bicou.redmine.sync;

import android.content.Context;
import android.text.TextUtils;

import net.bicou.redmine.Constants;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssueCategoriesList;
import net.bicou.redmine.data.json.IssuePrioritiesList;
import net.bicou.redmine.data.json.IssueStatusesList;
import net.bicou.redmine.data.json.IssuesList;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.ProjectsList;
import net.bicou.redmine.data.json.TrackersList;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.json.UsersList;
import net.bicou.redmine.data.json.VersionsList;
import net.bicou.redmine.data.json.WikiPagesIndex;
import net.bicou.redmine.data.sqlite.QueriesList;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		final List<NameValuePair> args = new ArrayList<NameValuePair>();
		args.add(new BasicNameValuePair("include", "trackers,issue_categories"));
		return new JsonDownloader<ProjectsList>(ProjectsList.class).fetchObject(ctx, server, "projects.json", args);
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

	public static UsersList syncUsersHack(Context ctx, Server server, Project project) {
		// TODO: this fails because the page is not designed to work as a REST API so it ignores the key paramter
		/*
		Here's an example of the HTML code of /watchers/autocomplete_for_user:

	<label><input id="watcher_user_ids_" name="watcher[user_ids][]" type="checkbox" value="3" /> Benoit Duffez</label>
	<label><input id="watcher_user_ids_" name="watcher[user_ids][]" type="checkbox" value="8" /> test test</label>
		 */

		String uri = "watchers/autocomplete_for_user";
		String html = new JsonDownloader<String>(String.class).fetchObject(ctx, server, uri);
		if (TextUtils.isEmpty(html)) {
			return null;
		}

		String[] lines = html.split("\n");
		Pattern userId = Pattern.compile("value=\"([0-9]+)\"");
		Pattern userName = Pattern.compile("/>([^<]+)");

		UsersList usersList = new UsersList();
		List<User> users = new ArrayList<User>();

		Matcher matcher;
		String[] name;
		User user;
		long uid;
		for (String line : lines) {
			matcher = userId.matcher(line);
			if (matcher.find()) {
				uid = Long.parseLong(matcher.group(1));
				matcher = userName.matcher(line);
				if (matcher.find()) {
					user = new User();
					user.id = uid;
					name = matcher.group(1).trim().split(" ");
					user.firstname = name[0].trim();
					name[0] = "";
					user.lastname = Util.join(name, " ").trim();
					users.add(user);
				}
			}
		}

		usersList.addObjects(users);
		return usersList;
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
