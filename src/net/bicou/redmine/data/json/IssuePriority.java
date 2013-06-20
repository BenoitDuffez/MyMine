package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssuePrioritiesDbAdapter;
import net.bicou.redmine.util.L;

public class IssuePriority extends Reference {
	public boolean is_default;
	public Server server;

	public IssuePriority(Server s, final Cursor c, final DbAdapter db) {
		server = s;
		for (final String col : IssuePrioritiesDbAdapter.ISSUE_CATEGORY_FIELDS) {
			try {
				final int columnIndex = c.getColumnIndex(col);
				if (col.equals(IssuePrioritiesDbAdapter.KEY_IS_DEFAULT)) {
					is_default = Integer.parseInt(c.getString(columnIndex)) > 0;
				} else if (col.equals(IssuePrioritiesDbAdapter.KEY_ID)) {
					id = c.getLong(columnIndex);
				} else if (col.equals(IssuePrioritiesDbAdapter.KEY_NAME)) {
					name = c.getString(columnIndex);
				} else if (col.equals(IssuePrioritiesDbAdapter.KEY_SERVER_ID)) {
				} else {
					L.e("Unhandled column " + col + "!", null);
				}
			} catch (final NumberFormatException nfe) {
				L.e("Unable to get issue priority is default from cursor", nfe);
			} catch (final Exception e) {
				L.e("Unable to get issue priority from cursor", e);
			}
		}
	}
}
