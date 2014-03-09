package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ProjectsDbAdapter extends DbAdapter {
	public static final String TABLE_PROJECTS = "projects";

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = "name";
	public static final String KEY_CREATED_ON = "created_on";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_UPDATED_ON = "updated_on";
	public static final String KEY_IDENTIFIER = "identifier";
	public static final String KEY_PARENT_ID = "parent";
	public static final String KEY_IS_FAVORITE = "is_favorite";
	public static final String KEY_IS_SYNC_BLOCKED = "is_sync_blocked";

	public static final String KEY_SERVER_ID = "server_id";
	public static final String[] PROJECT_FIELDS = new String[] {
			KEY_ID,
			KEY_NAME,
			KEY_DESCRIPTION,
			KEY_IDENTIFIER,
			KEY_PARENT_ID,
			KEY_CREATED_ON,
			KEY_UPDATED_ON,
			KEY_SERVER_ID,
			KEY_IS_FAVORITE,
			KEY_IS_SYNC_BLOCKED,
	};

	public static final String TABLE_PROJECTS_TRACKERS = "projects_trackers";
	public static final String KEY_PROJECTS_TRACKERS_SERVER_ID = "server_id";
	public static final String KEY_PROJECTS_TRACKERS_PROJECT_ID = "project_id";
	public static final String KEY_PROJECTS_TRACKERS_TRACKER_ID = "tracker_id";
	public static final String KEY_PROJECTS_TRACKERS_TRACKER_NAME = "tracker_name";

	public static final String[] PROJECTS_TRACKERS_FIELDS = new String[] {
			KEY_PROJECTS_TRACKERS_SERVER_ID,
			KEY_PROJECTS_TRACKERS_PROJECT_ID,
			KEY_PROJECTS_TRACKERS_TRACKER_ID,
			KEY_PROJECTS_TRACKERS_TRACKER_NAME
	};

	public static final String TABLE_PROJECTS_ISSUE_CATEGORIES = "projects_issue_categories";
	public static final String KEY_PROJECTS_ISSUE_CATEGORIES_SERVER_ID = "server_id";
	public static final String KEY_PROJECTS_ISSUE_CATEGORIES_PROJECT_ID = "project_id";
	public static final String KEY_PROJECTS_ISSUE_CATEGORIES_ISSUE_CATEGORY_ID = "issue_category_id";
	public static final String KEY_PROJECTS_ISSUE_CATEGORIES_ISSUE_CATEGORY_NAME = "issue_category_name";

	public static final String[] PROJECTS_ISSUE_CATEGORIES_FIELDS = new String[] {
			KEY_PROJECTS_ISSUE_CATEGORIES_SERVER_ID,
			KEY_PROJECTS_ISSUE_CATEGORIES_PROJECT_ID,
			KEY_PROJECTS_ISSUE_CATEGORIES_ISSUE_CATEGORY_ID,
			KEY_PROJECTS_ISSUE_CATEGORIES_ISSUE_CATEGORY_NAME
	};

	/**
	 * Table creation statements
	 */
	public static String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_PROJECTS + "(" + Util.join(PROJECT_FIELDS, ", ") + ", PRIMARY KEY (" + KEY_ID + ", " + KEY_SERVER_ID + "))",
				"CREATE TABLE " + TABLE_PROJECTS_TRACKERS + "(" + Util.join(PROJECTS_TRACKERS_FIELDS, ", ") + ", PRIMARY KEY (" + KEY_PROJECTS_TRACKERS_SERVER_ID + ", " +
						"" + KEY_PROJECTS_TRACKERS_PROJECT_ID + ", " + KEY_PROJECTS_TRACKERS_TRACKER_ID + "))",
				"CREATE TABLE " + TABLE_PROJECTS_ISSUE_CATEGORIES + "(" + Util.join(PROJECTS_ISSUE_CATEGORIES_FIELDS, ", ") + ", " +
						"" + "PRIMARY KEY (" + KEY_PROJECTS_ISSUE_CATEGORIES_SERVER_ID + ", " + KEY_PROJECTS_ISSUE_CATEGORIES_PROJECT_ID + ", " +
						"" + KEY_PROJECTS_ISSUE_CATEGORIES_ISSUE_CATEGORY_ID + "))",
		};
	}

	public ProjectsDbAdapter(final Context ctx) {
		super(ctx);
	}

	public ProjectsDbAdapter(final DbAdapter db) {
		super(db);
	}

	private void putValues(ContentValues values, Project project) {
		values.put(KEY_NAME, project.name);
		values.put(KEY_DESCRIPTION, project.description);
		values.put(KEY_IDENTIFIER, project.identifier);
		values.put(KEY_PARENT_ID, project.parent == null ? 0 : project.parent.id);
		values.put(KEY_CREATED_ON, project.created_on == null ? 0 : project.created_on.getTimeInMillis());
		values.put(KEY_UPDATED_ON, project.updated_on == null ? 0 : project.updated_on.getTimeInMillis());
		values.put(KEY_SERVER_ID, project.server.rowId);
		values.put(KEY_IS_FAVORITE, project.is_favorite ? 1 : 0);
		values.put(KEY_IS_SYNC_BLOCKED, project.is_sync_blocked ? 1 : 0);
	}

	public long insert(final Project project) {
		final ContentValues values = new ContentValues();
		values.put(KEY_ID, project.id);
		putValues(values, project);
		return mDb.insert(TABLE_PROJECTS, "", values);
	}

	public int update(final Project project) {
		final ContentValues values = new ContentValues();
		putValues(values, project);
		return mDb.update(TABLE_PROJECTS, values, KEY_ID + "=" + project.id, null);
	}

	public Project select(final Server server, final long projectId) {
		return select(server, projectId, null);
	}

	public Project select(final Server server, final long projectId, final String[] columns) {
		return select(server == null ? 0 : server.rowId, projectId, columns);
	}

	public Project select(final long serverId, final long projectId, final String[] columns) {
		String selection = KEY_ID + " = " + projectId;
		if (serverId > 0) {
			selection += " AND " + KEY_SERVER_ID + " = " + serverId;
		}

		final Cursor c = selectAllCursor(serverId, columns, selection);
		Project project = null;
		if (c != null) {
			if (c.moveToFirst()) {
				project = new Project(c);
			}
			c.close();
		}

		return project;
	}

	public Cursor selectAllCursor(final long serverId, final String[] desiredColumns, String where) {
		final Cursor c;
		String[] columns = desiredColumns == null ? PROJECT_FIELDS : desiredColumns;

		final List<String> tables = new ArrayList<String>();
		final List<String> cols = new ArrayList<String>();
		final List<String> args = new ArrayList<String>();
		final List<String> onArgs = new ArrayList<String>();

		tables.add(TABLE_PROJECTS);
		if (serverId > 0) {
			args.add(TABLE_PROJECTS + "." + KEY_SERVER_ID + " = " + serverId);
		}

		// Handle fake columns
		for (final String col : columns) {
			onArgs.clear();
			if (KEY_SERVER_ID.equals(col)) {
				for (String serverCol : ServersDbAdapter.SERVER_FIELDS) {
					cols.add(ServersDbAdapter.TABLE_SERVERS + "." + serverCol + " AS " + ServersDbAdapter.TABLE_SERVERS + "_" + serverCol);
				}
				onArgs.add(ServersDbAdapter.TABLE_SERVERS + "." + DbAdapter.KEY_ROWID + " = " + TABLE_PROJECTS + "." + KEY_SERVER_ID);
				tables.add("LEFT JOIN " + ServersDbAdapter.TABLE_SERVERS + " ON " + Util.join(onArgs.toArray(), " AND "));
			}
			cols.add(TABLE_PROJECTS + "." + col);
		}

		if (!TextUtils.isEmpty(where)) {
			args.add(where);
		}

		final String selection = args.size() > 0 ? " WHERE " + Util.join(args.toArray(), " AND ") : "";
		final String sql = "SELECT " + Util.join(cols.toArray(), ", ") + " FROM " + Util.join(tables.toArray(), " ") + selection;
		c = mDb.rawQuery(sql, null);

		c.moveToFirst();
		return c;
	}

	public List<Project> selectAll() {
		return selectAll(null);
	}

	public List<Project> selectAll(Server server) {
		return selectAll(server, null);
	}

	public List<Project> selectAll(Server server, String where) {
		final Cursor c = selectAllCursor(server == null ? 0 : server.rowId, null, where);
		final List<Project> projects = new ArrayList<Project>();
		if (c != null) {
			if (c.moveToFirst()) {
				do {
					projects.add(new Project(c));
				} while (c.moveToNext());
			}
			c.close();
		}
		return projects;
	}

	/**
	 * Remove absolutely all projects
	 */
	public int deleteAll() {
		return mDb.delete(TABLE_PROJECTS, null, null);
	}

	/**
	 * Remove all the projects linked to this server ID
	 */
	public int deleteAll(final long serverId) {
		return mDb.delete(TABLE_PROJECTS, KEY_SERVER_ID + "=?", new String[] { Long.toString(serverId) });
	}

	/**
	 * Delete a single project
	 */
	public boolean delete(final Server server, final long projectId) {
		int nb = mDb.delete(TABLE_PROJECTS, KEY_ID + " = " + projectId + " AND " + KEY_SERVER_ID + " = " + server.rowId, null);
		final IssuesDbAdapter idb = new IssuesDbAdapter(this);
		nb += idb.deleteAll(server, projectId);

		final VersionsDbAdapter vdb = new VersionsDbAdapter(this);
		nb += vdb.deleteAll(server, projectId);

		final WikiDbAdapter wdb = new WikiDbAdapter(this);
		nb += wdb.deleteAll(server, projectId);

		return nb > 0;
	}

	public int getNumProjects() {
		final Cursor c = mDb.query(TABLE_PROJECTS, new String[] { "COUNT(*)" }, KEY_IS_SYNC_BLOCKED + " != 1", null, null, null, null);

		int nb = 0;
		if (c != null) {
			if (c.moveToFirst()) {
				nb = c.getInt(0);
			}
			c.close();
		}
		return nb;
	}

	public List<Project> getFavorites() {
		Cursor c = selectAllCursor(0, null, KEY_IS_FAVORITE + " > 0");
		List<Project> projects = new ArrayList<Project>();
		if (c.moveToFirst()) {
			do {
				projects.add(new Project(c));
			} while (c.moveToNext());
			c.close();
		}
		return projects;
	}

	/**
	 * Retrieve all projects which have {@link #KEY_IS_SYNC_BLOCKED} set to {@code != 0}
	 *
	 * @return The list of projects that are blocked from sync
	 */
	public List<Project> getBlockedProjects() {
		Cursor c = selectAllCursor(0, null, KEY_IS_SYNC_BLOCKED + " > 0");
		List<Project> projects = new ArrayList<Project>();
		if (c.moveToFirst()) {
			do {
				projects.add(new Project(c));
			} while (c.moveToNext());
			c.close();
		}
		return projects;
	}
}
