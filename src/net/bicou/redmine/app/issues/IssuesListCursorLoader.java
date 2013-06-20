package net.bicou.redmine.app.issues;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter.OrderColumn;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssuesList;
import net.bicou.redmine.data.sqlite.*;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.platform.IssuesManager;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader that will handle the initial DB query and Cursor creation
 *
 * @author bicou
 *
 */
public final class IssuesListCursorLoader extends SimpleCursorLoader {
	private static final String[] COLUMN_SELECTION = new String[] {
			IssuesDbAdapter.KEY_ID + " AS " + DbAdapter.KEY_ROWID,
			IssuesDbAdapter.KEY_SUBJECT,
			IssuesDbAdapter.KEY_FIXED_VERSION,
			IssuesDbAdapter.KEY_DESCRIPTION,
			IssuesDbAdapter.KEY_STATUS,
			IssuesDbAdapter.KEY_SERVER_ID,
			IssuesDbAdapter.KEY_PROJECT,
			IssueStatusesDbAdapter.KEY_IS_CLOSED,
	};
	private final IssuesDbAdapter mHelper;
	IssuesListFilter mFilter;
	List<OrderColumn> mColumnsOrder;

	public IssuesListCursorLoader(final Context context, final IssuesDbAdapter helper, final Bundle args) {
		super(context);
		mHelper = helper;
		if (args != null) {
			if (args.getBoolean(IssuesListFilter.KEY_HAS_FILTER, false)) {
				mFilter = new IssuesListFilter(args);
			}
			mColumnsOrder = args.getParcelableArrayList(IssuesOrderingFragment.KEY_COLUMNS_ORDER);
		}

		if (mColumnsOrder == null) {
			mColumnsOrder = OrderColumn.getDefaultOrder();
		}
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
			return mHelper.selectAllCursor(mFilter, COLUMN_SELECTION, mColumnsOrder);

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

		return mHelper.selectAllCursor(server, issueIds, COLUMN_SELECTION, mColumnsOrder);
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

		// Fetch issues
		do {
			issues = new JsonDownloader<IssuesList>(IssuesList.class).setDownloadAllIfList(false).fetchObject(ctx, server, "issues.json", args);
			if (issues == null) {
				break;
			}
			nbDownloaded += issues.getSize();
			offset += issues.getSize();
			args[1] = new BasicNameValuePair("offset", Integer.toString(offset));

			IssuesManager.updateIssues(ctx, server, issues.issues, 0, null);

			for (final Issue i : issues.issues) {
				matchingIssues.add(i.id);
			}
		} while (issues != null && nbDownloaded < issues.total_count);

		return matchingIssues;
	}
}
