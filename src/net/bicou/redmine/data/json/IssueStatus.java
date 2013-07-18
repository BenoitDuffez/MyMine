package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.util.L;

public class IssueStatus extends Reference {
	public boolean is_default;
	public boolean is_closed;

	public IssueStatus(final Cursor c, final DbAdapter db) {
		super(c);

		for (final String col : IssueStatusesDbAdapter.ISSUE_STATUS_FIELDS) {
			try {
				final int columnIndex = c.getColumnIndex(col);
				if (col.equals(IssueStatusesDbAdapter.KEY_IS_DEFAULT)) {
					is_default = c.getInt(columnIndex) > 0;
				} else if (col.equals(IssueStatusesDbAdapter.KEY_IS_CLOSED)) {
					is_closed = c.getInt(columnIndex) > 0;
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

	@Override
	public String toString() {
		return super.toString() + ":" + getClass().getSimpleName() + " { is_default: " + is_default + ", is_closed: " + is_closed + " }";
	}
}
