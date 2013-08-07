package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssuePriority;
import net.bicou.redmine.data.json.Reference;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;

public class IssuePrioritiesDbAdapter extends DbAdapter {
	public static final String TABLE_ISSUE_PRIORITIES = "issue_priorities";

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = Reference.KEY_NAME;
	public static final String KEY_IS_DEFAULT = "is_default";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] ISSUE_CATEGORY_FIELDS = new String[] {
			KEY_ID,
			KEY_NAME,
			KEY_IS_DEFAULT,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_ISSUE_PRIORITIES //
						+ "(" + Util.join(ISSUE_CATEGORY_FIELDS, ", ") //
						+ ", PRIMARY KEY (" + KEY_ID + "," + KEY_SERVER_ID + "))",
		};
	}

	public IssuePrioritiesDbAdapter(final Context ctx) {
		super(ctx);
	}

	public IssuePrioritiesDbAdapter(final DbAdapter db) {
		super(db);
	}

	public static String getFieldAlias(String field, String prefix) {
		return TABLE_ISSUE_PRIORITIES + "." + field + " AS " + prefix + "_" + field;
	}

	private void putValues(ContentValues values, IssuePriority issuePriority) {
		values.put(KEY_ID, issuePriority.id);
		values.put(KEY_NAME, issuePriority.name);
		values.put(KEY_IS_DEFAULT, issuePriority.is_default);
		values.put(KEY_SERVER_ID, issuePriority.server.rowId);
	}

	public long insert(final IssuePriority issuePriority) {
		final ContentValues values = new ContentValues();
		putValues(values, issuePriority);
		return mDb.insert(TABLE_ISSUE_PRIORITIES, "", values);
	}

	public int update(final IssuePriority issuePriority) {
		final ContentValues values = new ContentValues();
		putValues(values, issuePriority);
		String selection = Util.join(new String[] {
				KEY_ID + " = " + issuePriority.id,
				KEY_SERVER_ID + " = " + issuePriority.server.rowId
		}, " AND ");
		return mDb.update(TABLE_ISSUE_PRIORITIES, values, selection, null);
	}

	public Cursor selectCursor(final Server server, final long id, final String[] columns) {
		String selection = Util.join(new String[] {
				KEY_ID + " = " + id,
				KEY_SERVER_ID + " = " + server.rowId
		}, " AND ");
		return mDb.query(TABLE_ISSUE_PRIORITIES, columns, selection, null, null, null, null);
	}

	public IssuePriority select(final Server server, final long rowId, final String[] columns) {
		final Cursor c = selectCursor(server, rowId, columns);
		IssuePriority issuePriority = null;
		if (c != null) {
			if (c.moveToFirst()) {
				issuePriority = new IssuePriority(server, c);
			}
			c.close();
		}

		return issuePriority;
	}

	/**
	 * Removes issues
	 */
	public int deleteAll(final Server server) {
		return mDb.delete(TABLE_ISSUE_PRIORITIES, KEY_SERVER_ID + " = " + server.rowId, null);
	}

	public ArrayList<IssuePriority> selectAll(final Server server) {
		String condition = KEY_SERVER_ID + " = " + server.rowId;
		final Cursor c = mDb.query(TABLE_ISSUE_PRIORITIES, null, condition, null, null, null, null);
		ArrayList<IssuePriority> priorities = null;
		if (c != null) {
			if (c.moveToFirst()) {
				priorities = new ArrayList<IssuePriority>();
				do {
					priorities.add(new IssuePriority(server, c));
				} while (c.moveToNext());
			}
			c.close();
		}
		return priorities;
	}
}
