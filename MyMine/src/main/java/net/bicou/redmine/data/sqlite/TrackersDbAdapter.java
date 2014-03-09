package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Tracker;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;

public class TrackersDbAdapter extends DbAdapter {
	public static final String TABLE_TRACKERS = "trackers";

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = "name";
	public static final String KEY_PROJECT_ID = "project_id";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] TRACKER_FIELDS = new String[] {
			KEY_ID,
			KEY_NAME,
			KEY_PROJECT_ID,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_TRACKERS + "(" + Util.join(TRACKER_FIELDS, ", ") + ", PRIMARY KEY (" +
						Util.join(new String[] {
								KEY_ID,
								KEY_SERVER_ID,
								KEY_PROJECT_ID
						}, ", ") + "))",
		};
	}

	public TrackersDbAdapter(final Context ctx) {
		super(ctx);
	}

	public TrackersDbAdapter(final DbAdapter db) {
		super(db);
	}

	public static String getFieldAlias(String field, String col) {
		return TABLE_TRACKERS + "." + field + " AS " + col + "_" + field;
	}

	private void putValues(final Server server, final Project project, final ContentValues values, Tracker tracker) {
		values.put(KEY_ID, tracker.id);
		values.put(KEY_NAME, tracker.name);
		values.put(KEY_PROJECT_ID, project.id);
		values.put(KEY_SERVER_ID, server.rowId);
	}

	public long insert(final Server server, final Project project, final Tracker tracker) {
		final ContentValues values = new ContentValues();
		putValues(server, project, values, tracker);
		return mDb.insert(TABLE_TRACKERS, "", values);
	}

	public int update(final Project project, final Tracker tracker) {
		final ContentValues values = new ContentValues();
		putValues(tracker.server, project, values, tracker);
		final String where = KEY_ID + " = " + tracker.id + " AND " + KEY_SERVER_ID + " = " + tracker.server.rowId;
		return mDb.update(TABLE_TRACKERS, values, where, null);
	}

	public int deleteAll(final Server server) {
		return deleteAll(server, 0);
	}

	public int deleteAll(final Server server, long projectId) {
		String where = KEY_SERVER_ID + " = " + server.rowId;
		if (projectId > 0) {
			where += " AND " + KEY_PROJECT_ID + " = " + projectId;
		}
		return mDb.delete(TABLE_TRACKERS, where, null);
	}

	public ArrayList<Tracker> selectAll(final Server server, long projectId) {
		final String selection = KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_PROJECT_ID + " = " + projectId;
		final Cursor c = mDb.query(TABLE_TRACKERS, TRACKER_FIELDS, selection, null, null, null, null);
		final ArrayList<Tracker> list = new ArrayList<Tracker>();

		if (c != null) {
			while (c.moveToNext()) {
				list.add(new Tracker(server, c));
			}
			c.close();
		}

		return list;
	}

	public Tracker select(final Server server, final long id) {
		if (server == null) {
			return null;
		}

		final String selection = KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_ID + " = " + id;
		final Cursor c = mDb.query(TABLE_TRACKERS, TRACKER_FIELDS, selection, null, null, null, null);
		Tracker tracker = null;

		if (c != null) {
			if (c.moveToFirst()) {
				tracker = new Tracker(server, c);
			}
			c.close();
		}

		return tracker;
	}
}
