package net.bicou.redmine.data.json;

import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.util.L;
import android.database.Cursor;

public class IssueStatus extends Reference {
	public boolean is_default;
	public boolean is_closed;

	public IssueStatus(final Cursor c, final DbAdapter db) {
		for (final String col : IssueStatusesDbAdapter.ISSUE_STATUS_FIELDS) {
			try {
				final int columnIndex = c.getColumnIndex(IssueStatusesDbAdapter.KEY_IS_DEFAULT);
				if (col.equals(IssueStatusesDbAdapter.KEY_IS_DEFAULT)) {
					is_default = Integer.parseInt(c.getString(columnIndex)) > 0;
				} else if (col.equals(IssueStatusesDbAdapter.KEY_IS_CLOSED)) {
					is_closed = Integer.parseInt(c.getString(columnIndex)) > 0;
				} else if (col.equals(IssueStatusesDbAdapter.KEY_ID)) {
					id = c.getLong(columnIndex);
				} else if (col.equals(IssueStatusesDbAdapter.KEY_NAME)) {
					name = c.getString(columnIndex);
				} else if (col.equals(IssueStatusesDbAdapter.KEY_SERVER_ID)) {
				} else {
					L.e("Unhandled column " + col + "!", null);
				}
			} catch (final NumberFormatException nfe) {
				L.e("Unable to get issue is default from cursor", nfe);
			} catch (final Exception e) {
				L.e("Unable to get issue is default from cursor", e);
			}
		}
	}
}
