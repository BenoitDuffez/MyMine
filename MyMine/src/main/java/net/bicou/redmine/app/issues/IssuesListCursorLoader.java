package net.bicou.redmine.app.issues;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import net.bicou.redmine.app.issues.order.IssuesOrder;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssuesList;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.SimpleCursorLoader;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.platform.IssuesManager;
import net.bicou.redmine.util.L;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader that will handle the initial DB query and Cursor creation
 *
 * @author bicou
 */
public final class IssuesListCursorLoader extends SimpleCursorLoader {
	private static final String[] COLUMN_SELECTION = new String[] {
			IssuesDbAdapter.KEY_ID + " AS " + DbAdapter.KEY_ROWID,
			IssuesDbAdapter.KEY_SUBJECT,
			IssuesDbAdapter.KEY_FIXED_VERSION,
			IssuesDbAdapter.KEY_FIXED_VERSION_ID,
			IssuesDbAdapter.KEY_DESCRIPTION,
			IssuesDbAdapter.KEY_STATUS,
			IssuesDbAdapter.KEY_STATUS_ID,
			IssuesDbAdapter.KEY_SERVER_ID,
			IssuesDbAdapter.KEY_PROJECT,
			IssuesDbAdapter.KEY_PROJECT_ID,
			IssuesDbAdapter.KEY_IS_FAVORITE,
			IssueStatusesDbAdapter.KEY_IS_CLOSED,
	};
	private final IssuesDbAdapter mHelper;
	IssuesListFilter mFilter;
	IssuesOrder mColumnsOrder;

	public IssuesListCursorLoader(final Context context, final IssuesDbAdapter helper, final Bundle args) {
		super(context);
		mHelper = helper;
		mFilter = IssuesListFilter.fromBundle(args);
		mColumnsOrder = IssuesOrder.fromBundle(args);
	}

	@Override
	public Cursor loadInBackground() {
		if (mFilter == null) {
			mFilter = IssuesListFilter.FILTER_ALL;
		}

		switch (mFilter.type) {
		default:
		case PROJECT:
		case ALL:
		case SEARCH:
		case VERSION:
			return mHelper.selectAllCursor(mFilter, COLUMN_SELECTION, mColumnsOrder.getColumns());

		case QUERY:
			return selectIssuesFromQuery();
		}
	}

	private Cursor selectIssuesFromQuery() {
		final Context ctx = getContext();

		// Fetch server
		final ServersDbAdapter db = new ServersDbAdapter(ctx);
		db.open();
		final Server server = db.getServer(mFilter.serverId);
		db.close();

		if (server == null) {
			return null;
		}

		// Update issues
		final List<Long> issueIds = updateMatchingIssues(ctx, server);

		return mHelper.selectAllCursor(server, issueIds, COLUMN_SELECTION, mColumnsOrder.getColumns());
	}

	/**
	 * Download the issues that match the given query from the given server. This will trigger a mini-sync on those issues. The list of the issues
	 */
	private List<Long> updateMatchingIssues(final Context ctx, final Server server) {
		int offset = 0;
		int nbDownloaded = 0;
		IssuesList issues;
		final NameValuePair[] args = {
				new BasicNameValuePair("query_id", Long.toString(mFilter.id)),
				new BasicNameValuePair("offset", "0"),
		};
		final List<Long> matchingIssues = new ArrayList<Long>();

		IssuesDbAdapter db = new IssuesDbAdapter(ctx);
		db.open();

		// Get URL, which may be linked to a project
		QueriesDbAdapter qdb = new QueriesDbAdapter(db);
		Query query = qdb.select(server, mFilter.id, null);
		final String url;
		if (query != null && query.project_id > 0) {
			url = "projects/" + query.project_id + "/issues.json";
		} else {
			url = "issues.json";
		}

		// Fetch issues
		do {
			issues = new JsonDownloader<IssuesList>(IssuesList.class).setDownloadAllIfList(false).fetchObject(ctx, server, url, args);
			if (issues == null) {
				break;
			}
			nbDownloaded += issues.getSize();
			offset += issues.getSize();
			args[1] = new BasicNameValuePair("offset", Integer.toString(offset));

			IssuesManager.updateIssues(db, server, issues.issues, 0, null);

			for (final Issue i : issues.issues) {
				matchingIssues.add(i.id);
			}
		} while (issues != null && nbDownloaded < issues.total_count);

		db.close();

		return matchingIssues;
	}
}
