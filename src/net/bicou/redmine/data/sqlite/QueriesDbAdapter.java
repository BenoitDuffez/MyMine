package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;

public class QueriesDbAdapter extends DbAdapter {
	public static final String TABLE_QUERIES = "queries";

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = "name";
	public static final String KEY_IS_PUBLIC = "is_public";
	public static final String KEY_PROJECT_ID = "project_id";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] QUERY_FIELDS = new String[] {
			KEY_ID,
			KEY_NAME,
			KEY_IS_PUBLIC,
			KEY_PROJECT_ID,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_QUERIES + "(" + Util.join(QUERY_FIELDS, ", ") + ", PRIMARY KEY (" + KEY_ID + ", " + KEY_SERVER_ID + "))",
		};
	}

	public QueriesDbAdapter(final Context ctx) {
		super(ctx);
	}

	public QueriesDbAdapter(final DbAdapter db) {
		super(db);
	}

	private void feedValues(final ContentValues values, final Query query) {
		values.put(KEY_ID, query.id);
		values.put(KEY_NAME, query.name);
		values.put(KEY_IS_PUBLIC, query.is_public ? 0 : 1);
		values.put(KEY_PROJECT_ID, query.project == null ? 0 : query.project.id);
	}

	public long insert(final Query query) {
		final ContentValues values = new ContentValues();
		feedValues(values, query);
		values.put(KEY_SERVER_ID, query.server.rowId);
		return mDb.insert(TABLE_QUERIES, "", values);
	}

	public int update(final Query query) {
		final ContentValues values = new ContentValues();
		feedValues(values, query);
		return mDb.update(TABLE_QUERIES, values, KEY_ID + "=" + query.id + " AND " + KEY_SERVER_ID + " = " + query.server.rowId, null);
	}

	public Cursor selectCursor(final Server server, final long id, final String[] columns) {
		final String where = KEY_ID + " = " + id + " AND " + KEY_SERVER_ID + " = " + server.rowId;
		return mDb.query(TABLE_QUERIES, columns, where, null, null, null, null);
	}

	public Query select(final Server server, final long queryId, final String[] columns) {
		final Cursor c = selectCursor(server, queryId, columns);
		Query query = null;
		if (c != null) {
			if (c.moveToFirst()) {
				query = new Query(server, c, this);
			}
			c.close();
		}

		return query;
	}

	public Cursor selectAllCursor(final Server server, final String[] columns) {
		final Cursor c;

		String selection = "";
		if (server != null) {
			selection = KEY_SERVER_ID + " = " + server.rowId;
		}
		final String[] selArgs = null;

		c = mDb.query(TABLE_QUERIES, columns, selection, selArgs, null, null, null);

		c.moveToFirst();
		return c;
	}

	public List<Query> selectAll(final Server server) {
		final Cursor c = selectAllCursor(server, null);
		final List<Query> queries = new ArrayList<Query>();
		if (c != null) {
			while (c.moveToNext()) {
				queries.add(new Query(server, c, this));
			}
			c.close();
		}

		return queries;
	}

	public void saveAll(final Server server, final long projectId, final List<Query> queries) {
		mDb.beginTransaction();
		try {
			deleteAll(server, projectId);

			for (final Query query : queries) {
				insert(query);
			}

			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}

	/**
	 * Removes queries
	 */
	public int deleteAll(final Server server, final long projectId) {
		final List<String> selection = new ArrayList<String>();
		if (server != null) {
			selection.add(KEY_SERVER_ID + " = " + server.rowId);
		}
		if (projectId > 0) {
			selection.add(KEY_PROJECT_ID + " = " + projectId);
		}
		return mDb.delete(TABLE_QUERIES, Util.join(selection.toArray(), " AND "), null);
	}
}
