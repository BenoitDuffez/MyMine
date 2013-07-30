package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VersionsDbAdapter extends DbAdapter {
	public static final String TABLE_VERSIONS = "versions";

	public static final String KEY_ID = "id";
	public static final String KEY_PROJECT_ID = "project_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_STATUS = "status";
	public static final String KEY_DUE_DATE = "due_date";
	public static final String KEY_SHARING = "sharing";
	public static final String KEY_CREATED_ON = "created_on";
	public static final String KEY_UPDATED_ON = "updated_on";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] VERSION_FIELDS = new String[] {
			KEY_ID,
			KEY_PROJECT_ID,
			KEY_NAME,
			KEY_DESCRIPTION,
			KEY_STATUS,
			KEY_DUE_DATE,
			KEY_SHARING,
			KEY_CREATED_ON,
			KEY_UPDATED_ON,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_VERSIONS + " (" + Util.join(VERSION_FIELDS, ", ") + ", " //
						+ "PRIMARY KEY (" + KEY_ID + ", " + KEY_SERVER_ID + ", " + KEY_PROJECT_ID + "))",
		};
	}

	public VersionsDbAdapter(final Context ctx) {
		super(ctx);
	}

	public VersionsDbAdapter(final DbAdapter db) {
		super(db);
	}

	private void putValues(final Server server, final ContentValues values, final Version version) {
		values.put(KEY_PROJECT_ID, version.project == null ? 0 : version.project.id);
		values.put(KEY_NAME, version.name);
		values.put(KEY_DESCRIPTION, version.description);
		values.put(KEY_STATUS, version.status.name().toLowerCase(Locale.ENGLISH));
		values.put(KEY_DUE_DATE, version.due_date == null ? 0 : version.due_date.getTimeInMillis());
		values.put(KEY_SHARING, version.sharing);
		values.put(KEY_CREATED_ON, version.created_on == null ? 0 : version.created_on.getTimeInMillis());
		values.put(KEY_UPDATED_ON, version.updated_on == null ? 0 : version.updated_on.getTimeInMillis());
		values.put(KEY_SERVER_ID, server.rowId);
	}

	public long insert(final Server server, final Version version) {
		final ContentValues values = new ContentValues();
		values.put(KEY_ID, version.id);
		putValues(server, values, version);
		return mDb.insert(TABLE_VERSIONS, "", values);
	}

	public int update(final Server server, final Version version) {
		final ContentValues values = new ContentValues();
		putValues(server, values, version);
		return mDb.update(TABLE_VERSIONS, values, KEY_ID + "=" + version.id, null);
	}

	public Cursor selectCursor(final Server server, final Project project, final long rowId, final String[] columns) {
		final String where = KEY_ID + " = " + rowId + " AND " + KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_PROJECT_ID + " = " + project.id;
		return mDb.query(TABLE_VERSIONS, columns, where, null, null, null, null);
	}

	public int deleteAll(final Server server, final long projectId) {
		String where = KEY_SERVER_ID + " = " + server.rowId;
		where += projectId > 0 ? " AND " + KEY_PROJECT_ID + " = " + projectId : "";
		return mDb.delete(TABLE_VERSIONS, where, null);
	}

	public List<Version> selectAll(Server server, Project project) {
		final String where = KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_PROJECT_ID + " = " + project.id;
		String orderBy = Util.join(new String[] {
				KEY_STATUS + " DESC",
				KEY_DUE_DATE + " DESC",
		}, ", ");
		List<Version> versions = new ArrayList<Version>();
		Cursor c = mDb.query(TABLE_VERSIONS, null, where, null, null, null, orderBy);
		if (c != null) {
			while (c.moveToNext()) {
				versions.add(new Version(c));
			}
			c.close();
		}
		return versions;
	}

	public String getName(final Server server, final Project project, final long id) {
		final Cursor c = selectCursor(server, project, id, new String[] {
				KEY_PROJECT_ID,
				KEY_NAME,
		});

		String statusName = null;
		if (c != null) {
			if (c.moveToFirst()) {
				statusName = c.getString(c.getColumnIndex(KEY_NAME));
			}
			c.close();
		}

		return statusName;
	}
}
