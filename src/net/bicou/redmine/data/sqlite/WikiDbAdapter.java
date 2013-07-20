package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;

public class WikiDbAdapter extends DbAdapter {
	public static final String TABLE_WIKI = "wiki";

	public static final String KEY_PROJECT_ID = "project_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_TEXT = "text";
	public static final String KEY_VERSION = "version";
	public static final String KEY_AUTHOR_ID = "author_id";
	public static final String KEY_COMMENTS = "comments";
	public static final String KEY_CREATED_ON = "created_on";
	public static final String KEY_UPDATED_ON = "updated_on";
	public static final String KEY_IS_FAVORITE = "is_favorite";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] WIKI_FIELDS = new String[] {
			KEY_ROWID,
			KEY_PROJECT_ID,
			KEY_TITLE,
			KEY_TEXT,
			KEY_VERSION,
			KEY_AUTHOR_ID,
			KEY_COMMENTS,
			KEY_CREATED_ON,
			KEY_UPDATED_ON,
			KEY_IS_FAVORITE,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		String fields = KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT";
		fields += Util.join(WIKI_FIELDS, ", ").substring(KEY_ROWID.length());
		return new String[] {
				"CREATE TABLE " + TABLE_WIKI + "(" + fields + ", UNIQUE (" + KEY_TITLE + ", " + KEY_SERVER_ID + ", " +
						"" + KEY_PROJECT_ID + "))",
		};
	}

	public WikiDbAdapter(final Context ctx) {
		super(ctx);
	}

	public WikiDbAdapter(final DbAdapter db) {
		super(db);
	}

	private void putValues(final Server server, final Project project, final ContentValues values, final WikiPage page) {
		values.put(KEY_PROJECT_ID, project.id);
		values.put(KEY_TITLE, page.title);
		values.put(KEY_TEXT, page.text);
		values.put(KEY_VERSION, page.version);
		values.put(KEY_AUTHOR_ID, page.author == null ? 0 : page.author.id);
		values.put(KEY_COMMENTS, page.comments);
		values.put(KEY_CREATED_ON, page.created_on == null ? 0 : page.created_on.getTimeInMillis());
		values.put(KEY_UPDATED_ON, page.updated_on == null ? 0 : page.updated_on.getTimeInMillis());
		values.put(KEY_SERVER_ID, server.rowId);
		values.put(KEY_IS_FAVORITE, page.is_favorite ? 1 : 0);
	}

	public long insert(final Server server, final Project project, final WikiPage page) {
		final ContentValues values = new ContentValues();
		putValues(server, project, values, page);
		return mDb.insert(TABLE_WIKI, "", values);
	}

	public int update(final WikiPage page) {
		final ContentValues values = new ContentValues();
		putValues(page.server, page.project, values, page);
		final String where = KEY_TITLE + " = ?" //
				+ " AND " + KEY_SERVER_ID + " = " + page.server.rowId //
				+ " AND " + KEY_PROJECT_ID + " = " + page.project.id;
		final String[] whereArgs = new String[] { page.title, };
		return mDb.update(TABLE_WIKI, values, where, whereArgs);
	}

	public int deleteAll(final Server server, final long projectId) {
		String where = KEY_SERVER_ID + " = " + server.rowId;
		where += projectId > 0 ? " AND " + KEY_PROJECT_ID + " = " + projectId : "";
		return mDb.delete(TABLE_WIKI, where, null);
	}

	public List<WikiPage> selectAll(final Server server, final Project project) {
		final String selection = KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_PROJECT_ID + " = " + project.id;
		final Cursor c = mDb.query(TABLE_WIKI, WIKI_FIELDS, selection, null, null, null, null);
		final List<WikiPage> list = new ArrayList<WikiPage>();

		if (c != null) {
			while (c.moveToNext()) {
				list.add(new WikiPage(c, server, project));
			}
			c.close();
		}

		return list;
	}

	public WikiPage select(final Server server, final Project project, final String uri) {
		if (server == null || project == null) {
			return null;
		}

		final String selection = KEY_SERVER_ID + " = " + server.rowId //
				+ " AND " + KEY_PROJECT_ID + " = " + project.id //
				+ " AND " + KEY_TITLE + " = ?";
		final String[] selArgs = new String[] { uri };

		final Cursor c = mDb.query(TABLE_WIKI, WIKI_FIELDS, selection, selArgs, null, null, null);
		WikiPage wikiPage = null;

		if (c != null) {
			if (c.moveToFirst()) {
				wikiPage = new WikiPage(c, server, project);
			}
			c.close();
		}

		return wikiPage;
	}

	public List<WikiPage> selectFavorites() {
		List<WikiPage> pages = new ArrayList<WikiPage>();
		Cursor c = mDb.query(TABLE_WIKI, null, KEY_IS_FAVORITE + " > 0", null, null, null, null);
		if (c.moveToFirst()) {
			do {
				pages.add(new WikiPage(c, this));
			} while (c.moveToNext());
		}
		return pages;
	}

	public Cursor selectAllCursor(final Server server, Project project, final String[] columns) {
		final Cursor c;

		if (columns == null) {
			List<String> selection = new ArrayList<String>();
			if (server != null) {
				selection.add(KEY_SERVER_ID + " = " + server.rowId);
			}
			if (project != null) {
				selection.add(KEY_PROJECT_ID + " = " + project.id);
			}
			c = mDb.query(TABLE_WIKI, columns, Util.join(selection.toArray(), " AND "), null, null, null, null);
		} else {
			final List<String> tables = new ArrayList<String>();
			final List<String> cols = new ArrayList<String>();
			final List<String> args = new ArrayList<String>();
			final List<String> onArgs = new ArrayList<String>();

			tables.add(TABLE_WIKI);
			if (server != null) {
				args.add(TABLE_WIKI + "." + KEY_SERVER_ID + " = " + server.rowId);
			}
			if (server != null) {
				args.add(TABLE_WIKI + "." + KEY_PROJECT_ID + " = " + project.id);
			}

			// Handle fake columns
			for (final String col : columns) {
				onArgs.clear();
				if (KEY_SERVER_ID.equals(col)) {
					cols.add(ServersDbAdapter.TABLE_SERVERS + "." + ServersDbAdapter.KEY_SERVER_URL + " AS " + col);
					onArgs.add(ServersDbAdapter.TABLE_SERVERS + "." + DbAdapter.KEY_ROWID + " = " + TABLE_WIKI + "." + KEY_SERVER_ID);
					tables.add("LEFT JOIN " + ServersDbAdapter.TABLE_SERVERS + " ON " + Util.join(onArgs.toArray(), " AND "));
				}
				cols.add(TABLE_WIKI + "." + col);
			}

			final String where = args.size() > 0 ? " WHERE " + Util.join(args.toArray(), " AND ") : "";
			final String sql = "SELECT " + Util.join(cols.toArray(), ", ") + " FROM " + Util.join(tables.toArray(), " ") + where;

			c = mDb.rawQuery(sql, null);
		}

		c.moveToFirst();
		return c;
	}
}
