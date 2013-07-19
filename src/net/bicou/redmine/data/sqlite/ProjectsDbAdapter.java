package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_PROJECTS + "(" + Util.join(PROJECT_FIELDS, ", ") + ", PRIMARY KEY (" + KEY_ID + ", " + KEY_SERVER_ID + "))",
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

		final Cursor c = mDb.query(TABLE_PROJECTS, columns, selection, null, null, null, null);
		Project project = null;
		if (c != null) {
			if (c.moveToFirst()) {
				project = new Project(c, this);
			}
			c.close();
		}

		return project;
	}

	public Cursor selectAllCursor(final Server server, final String[] columns) {
		final Cursor c;

		if (columns == null) {
			final String selection = server == null ? null : KEY_SERVER_ID + " = " + server.rowId;
			final String[] selArgs = null;
			c = mDb.query(TABLE_PROJECTS, columns, selection, selArgs, null, null, null);
		} else {
			final List<String> tables = new ArrayList<String>();
			final List<String> cols = new ArrayList<String>();
			final List<String> args = new ArrayList<String>();
			final List<String> onArgs = new ArrayList<String>();

			tables.add(TABLE_PROJECTS);
			if (server != null) {
				args.add(TABLE_PROJECTS + "." + KEY_SERVER_ID + " = " + server.rowId);
			}

			// Handle fake columns
			for (final String col : columns) {
				onArgs.clear();
				if (KEY_PARENT_ID.equals(col)) {
					cols.add(TABLE_PROJECTS + "2." + KEY_NAME + " AS " + col);
					onArgs.add(TABLE_PROJECTS + "2." + KEY_ID + " = " + TABLE_PROJECTS + "." + KEY_PARENT_ID);
					onArgs.add(TABLE_PROJECTS + "2." + KEY_SERVER_ID + " = " + TABLE_PROJECTS + "." + KEY_SERVER_ID);
					tables.add("LEFT JOIN " + TABLE_PROJECTS + " " + TABLE_PROJECTS + "2 ON " + Util.join(onArgs.toArray(), " AND "));
				} else if (KEY_SERVER_ID.equals(col)) {
					cols.add(ServersDbAdapter.TABLE_SERVERS + "." + ServersDbAdapter.KEY_SERVER_URL + " AS " + col);
					onArgs.add(ServersDbAdapter.TABLE_SERVERS + "." + DbAdapter.KEY_ROWID + " = " + TABLE_PROJECTS + "." + KEY_SERVER_ID);
					tables.add("LEFT JOIN " + ServersDbAdapter.TABLE_SERVERS + " ON " + Util.join(onArgs.toArray(), " AND "));
				}
				cols.add(TABLE_PROJECTS + "." + col);
			}

			final String where = args.size() > 0 ? " WHERE " + Util.join(args.toArray(), " AND ") : "";
			final String sql = "SELECT " + Util.join(cols.toArray(), ", ") + " FROM " + Util.join(tables.toArray(), " ") + where;
			c = mDb.rawQuery(sql, null);
		}

		c.moveToFirst();
		return c;
	}

	public List<Project> selectAll() {
		final Cursor c = mDb.query(TABLE_PROJECTS, null, null, null, null, null, null);
		final List<Project> projects = new ArrayList<Project>();
		if (c != null) {
			while (c.moveToNext()) {
				projects.add(new Project(c, this));
			}
			c.close();
		}
		return projects;
	}

	/**
	 * Removes absolutely all projects
	 */
	public int deleteAll() {
		return mDb.delete(TABLE_PROJECTS, null, null);
	}

	/**
	 * Removes all the projects linked to this server ID
	 */
	public int deleteAll(final long serverId) {
		return mDb.delete(TABLE_PROJECTS, KEY_SERVER_ID + "=?", new String[] {
				Long.toString(serverId)
		});
	}

	/**
	 * Deletes a single project
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
		final Cursor c = mDb.query(TABLE_PROJECTS, new String[] {
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

	public List<Project> getFavorites() {
		Cursor c = mDb.query(TABLE_PROJECTS, null, KEY_IS_FAVORITE + " > 0", null, null, null, null);
		List<Project> projects = new ArrayList<Project>();
		if (c.moveToFirst()) {
			projects.add(new Project(c, this));
			c.close();
		}
		return projects;
	}
}
