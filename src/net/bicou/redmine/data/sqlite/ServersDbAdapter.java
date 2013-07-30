package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ServersDbAdapter extends DbAdapter {
	public static final String TABLE_SERVERS = "servers";
	public static final String KEY_SERVER_URL = "url";
	public static final String KEY_API_KEY = "api_key";
	public static final String KEY_USER_ID = "user_id";
	public static final String KEY_AUTH_USERNAME = "auth_username";
	public static final String KEY_AUTH_PASSWORD = "auth_password";

	public static final String[] SERVER_FIELDS = {
			KEY_ROWID,
			KEY_SERVER_URL,
			KEY_API_KEY,
			KEY_USER_ID,
			KEY_AUTH_USERNAME,
			KEY_AUTH_PASSWORD,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_SERVERS + "(" + KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_SERVER_URL + "," + KEY_API_KEY + "," +
						"" + KEY_USER_ID + "," + KEY_AUTH_USERNAME + "," + KEY_AUTH_PASSWORD + ")",
		};
	}

	public ServersDbAdapter(final Context ctx) {
		super(ctx);
	}

	public ServersDbAdapter(final DbAdapter other) {
		super(other);
	}

	private String[] getAllColumns() {
		List<String> selection = new ArrayList<String>();
		for (String col : SERVER_FIELDS) {
			selection.add(col);
		}
		for (String col : UsersDbAdapter.USER_FIELDS) {
			selection.add(UsersDbAdapter.TABLE_USERS + "." + col + " AS " + UsersDbAdapter.TABLE_USERS + "_" + col);
		}
		return selection.toArray(new String[] { });
	}

	private String getDefaultSelection() {
		return Util.join(new String[] {
				UsersDbAdapter.TABLE_USERS + "." + UsersDbAdapter.KEY_ID + " = " + TABLE_SERVERS + "." + KEY_USER_ID,
				UsersDbAdapter.TABLE_USERS + "." + UsersDbAdapter.KEY_SERVER_ID + " = " + TABLE_SERVERS + "." + KEY_ROWID,
		}, " AND ");
	}

	private String getDefaultTables() {
		return TABLE_SERVERS + ", " + UsersDbAdapter.TABLE_USERS;
	}

	public List<Server> selectAll() {
		final Cursor c = mDb.query(getDefaultTables(), getAllColumns(), getDefaultSelection(), null, null, null, null);

		final List<Server> servers = new ArrayList<Server>();
		if (c != null) {
			while (c.moveToNext()) {
				servers.add(new Server(c));
			}
			c.close();
		}

		return servers;
	}

	public long insert(final Server server) {
		final ContentValues values = new ContentValues();
		values.put(KEY_SERVER_URL, server.serverUrl);
		values.put(KEY_API_KEY, server.apiKey);
		values.put(KEY_USER_ID, server.user == null ? 0 : server.user.id);
		values.put(KEY_AUTH_USERNAME, server.authUsername == null ? "" : server.authUsername);
		values.put(KEY_AUTH_PASSWORD, server.authPassword == null ? "" : server.authPassword);
		return mDb.insert(TABLE_SERVERS, "", values);
	}

	public Server getServer(final long rowId) {
		String selection = getDefaultSelection() + " AND " + KEY_ROWID + " = " + rowId;
		final Cursor c = mDb.query(getDefaultTables(), getAllColumns(), selection, null, null, null, null);

		Server server = null;
		if (c != null) {
			if (c.moveToFirst()) {
				server = new Server(c);
			}
			c.close();
		}
		return server;
	}

	public long getServerId(final String serverUrl) {
		final String[] cols = {
				KEY_ROWID
		};
		final String cond = KEY_SERVER_URL + " = ?";
		final String[] args = {
				serverUrl
		};
		final Cursor c = mDb.query(TABLE_SERVERS, cols, cond, args, null, null, null);

		long serverId = 0;
		if (c != null) {
			if (c.moveToFirst()) {
				serverId = c.getLong(0);
			}
			c.close();
		}
		return serverId;
	}

	public Server getServer(final String serverUrl) {
		final String[] cols = getAllColumns();
		final String cond = getDefaultSelection() + " AND " + KEY_SERVER_URL + " = ?";
		final String[] args = {
				serverUrl
		};
		final Cursor c = mDb.query(getDefaultTables(), cols, cond, args, null, null, null);

		Server server = null;
		if (c != null) {
			if (c.moveToFirst()) {
				server = new Server(c);
			}
			c.close();
		}
		return server;
	}

	public void delete(final long rowId) {
		mDb.delete(TABLE_SERVERS, KEY_ROWID + " = " + rowId, null);
		mDb.delete(IssuesDbAdapter.TABLE_ISSUES, IssuesDbAdapter.KEY_SERVER_ID + " = " + rowId, null);
		mDb.delete(IssueStatusesDbAdapter.TABLE_ISSUE_STATUSES, IssueStatusesDbAdapter.KEY_SERVER_ID + " = " + rowId, null);
		mDb.delete(ProjectsDbAdapter.TABLE_PROJECTS, ProjectsDbAdapter.KEY_SERVER_ID + " = " + rowId, null);
		mDb.delete(UsersDbAdapter.TABLE_USERS, UsersDbAdapter.KEY_SERVER_ID + " = " + rowId, null);
		mDb.delete(VersionsDbAdapter.TABLE_VERSIONS, VersionsDbAdapter.KEY_SERVER_ID + " = " + rowId, null);
		mDb.delete(WikiDbAdapter.TABLE_WIKI, WikiDbAdapter.KEY_SERVER_ID + " = " + rowId, null);
	}

	public int getNumServers() {
		final Cursor c = mDb.query(TABLE_SERVERS, new String[] {
				"COUNT(*)"
		}, null, null, null, null, null);

		int nb = 0;
		if (c != null) {
			if (c.moveToFirst()) {
				nb = c.getInt(0);
			}
			c.close();
		}
		return nb;
	}
}
