package net.bicou.redmine.data.json;

import android.database.Cursor;
import android.database.SQLException;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;

public class Query {
	public int id;
	public String name;
	public boolean is_public;
	public int project_id;
	public Server server;

	public Query(final Server server, final Cursor c, final DbAdapter db) {
		this.server = server;
		for (final String col : QueriesDbAdapter.QUERY_FIELDS) {
			final int columnIndex = c.getColumnIndex(col);
			try {
				if (col.equals(QueriesDbAdapter.KEY_ID)) {
					id = c.getInt(columnIndex);
				} else if (col.equals(QueriesDbAdapter.KEY_IS_PUBLIC)) {
					is_public = c.getInt(columnIndex) == 1;
				} else if (col.equals(QueriesDbAdapter.KEY_NAME)) {
					name = c.getString(columnIndex);
				} else if (col.equals(QueriesDbAdapter.KEY_PROJECT_ID)) {
					project_id = c.getInt(columnIndex);
				} else if (col.equals(QueriesDbAdapter.KEY_SERVER_ID)) {
					if (server == null) {
						final ServersDbAdapter sdb = new ServersDbAdapter(db);
						this.server = sdb.getServer(c.getLong(columnIndex));
					}
				} else {
					throw new IllegalAccessError("Unhandled column: " + col);
				}
			} catch (final SQLException e) {

			}
		}
	}
}
