package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssueStatus;
import net.bicou.redmine.data.json.Reference;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;

public class IssueStatusesDbAdapter extends DbAdapter {
	public static final String TABLE_ISSUE_STATUSES = "issue_statuses";

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = Reference.KEY_NAME;
	public static final String KEY_IS_DEFAULT = "is_default";
	public static final String KEY_IS_CLOSED = "is_closed";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] ISSUE_STATUS_FIELDS = new String[] {
			KEY_ID,
			KEY_NAME,
			KEY_IS_DEFAULT,
			KEY_IS_CLOSED,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_ISSUE_STATUSES //
						+ "(" + Util.join(ISSUE_STATUS_FIELDS, ", ") //
						+ ", PRIMARY KEY (" + KEY_ID + ", " + KEY_SERVER_ID + "))",
		};
	}

	public IssueStatusesDbAdapter(final Context ctx) {
		super(ctx);
	}

	public IssueStatusesDbAdapter(final DbAdapter db) {
		super(db);
	}

	public static String getFieldAlias(String field, String col) {
		return TABLE_ISSUE_STATUSES + "." + field + " AS " + col + "_" + field;
	}

	public long insert(final Server server, final IssueStatus issueStatus) {
		final ContentValues values = new ContentValues();
		values.put(KEY_ID, issueStatus.id);
		values.put(KEY_NAME, issueStatus.name);
		values.put(KEY_IS_DEFAULT, issueStatus.is_default ? "1" : "0");
		values.put(KEY_IS_CLOSED, issueStatus.is_closed ? "1" : "0");
		values.put(KEY_SERVER_ID, server.rowId);
		return mDb.insert(TABLE_ISSUE_STATUSES, "", values);
	}

	public int update(final Server server, final IssueStatus issueStatus) {
		final ContentValues values = new ContentValues();
		values.put(KEY_NAME, issueStatus.name);
		values.put(KEY_IS_DEFAULT, issueStatus.is_default ? "1" : "0");
		values.put(KEY_IS_CLOSED, issueStatus.is_closed ? "1" : "0");
		return mDb.update(TABLE_ISSUE_STATUSES, values, KEY_ID + "=" + issueStatus.id + " AND " + KEY_SERVER_ID + " = " + server.rowId, null);
	}

	public Cursor selectCursor(final long serverId, final long id, final String[] columns) {
		return mDb.query(TABLE_ISSUE_STATUSES, columns, KEY_ID + " = " + id + " AND " + KEY_SERVER_ID + " = " + serverId, null, null, null, null);
	}

	public IssueStatus select(final long serverId, final long rowId, final String[] columns) {
		final Cursor c = selectCursor(serverId, rowId, columns);
		IssueStatus issueStatus = null;
		if (c != null) {
			if (c.moveToFirst()) {
				issueStatus = new IssueStatus(c);
			}
			c.close();
		}

		return issueStatus;
	}

	/**
	 * Removes issues
	 */
	public int deleteAll(final Server server) {
		return mDb.delete(TABLE_ISSUE_STATUSES, KEY_SERVER_ID + " = " + server.rowId, null);
	}

	public String getName(final Server server, final long rowId) {
		final Cursor c = selectCursor(server.rowId, rowId, new String[] {
				KEY_NAME
		});

		String statusName = null;
		if (c != null) {
			if (c.moveToFirst()) {
				statusName = c.getString(0);
			}
			c.close();
		}

		return statusName;
	}

	public ArrayList<IssueStatus> selectAll(final Server server) {
		String condition = KEY_SERVER_ID + " = " + server.rowId;
		final Cursor c = mDb.query(TABLE_ISSUE_STATUSES, null, condition, null, null, null, null);
		ArrayList<IssueStatus> statuses = null;
		if (c != null) {
			if (c.moveToFirst()) {
				statuses = new ArrayList<IssueStatus>();
				do {
					statuses.add(new IssueStatus(c));
				} while (c.moveToNext());
			}
			c.close();
		}
		return statuses;
	}
}
