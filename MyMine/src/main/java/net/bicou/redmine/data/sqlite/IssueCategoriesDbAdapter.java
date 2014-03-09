package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssueCategory;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Reference;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;

public class IssueCategoriesDbAdapter extends DbAdapter {
	public static final String TABLE_ISSUE_CATEGORIES = "issue_categories";

	public static final String KEY_ID = "id";
	public static final String KEY_NAME = Reference.KEY_NAME;
	public static final String KEY_ASSIGNED_TO_ID = "assigned_to";

	public static final String KEY_SERVER_ID = "server_id";
	public static final String KEY_PROJECT_ID = "project_id";

	public static final String[] ISSUE_CATEGORY_FIELDS = new String[] {
			KEY_ID,
			KEY_NAME,
			KEY_ASSIGNED_TO_ID,
			KEY_PROJECT_ID,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_ISSUE_CATEGORIES //
						+ "(" + Util.join(ISSUE_CATEGORY_FIELDS, ", ") //
						+ ", PRIMARY KEY (" + KEY_ID + ", " + KEY_PROJECT_ID + "," + KEY_SERVER_ID + "))",
		};
	}

	public IssueCategoriesDbAdapter(final Context ctx) {
		super(ctx);
	}

	public IssueCategoriesDbAdapter(final DbAdapter db) {
		super(db);
	}

	public static String getFieldAlias(String field, final String col) {
		return TABLE_ISSUE_CATEGORIES + "." + field + " AS " + col + "_" + field;
	}

	private void putValues(ContentValues values, IssueCategory issueCategory) {
		values.put(KEY_ID, issueCategory.id);
		values.put(KEY_NAME, issueCategory.name);
		values.put(KEY_ASSIGNED_TO_ID, issueCategory.assigned_to == null ? 0 : issueCategory.assigned_to.id);
		values.put(KEY_PROJECT_ID, issueCategory.project.id);
		values.put(KEY_SERVER_ID, issueCategory.server.rowId);
	}

	public long insert(final IssueCategory issueCategory) {
		final ContentValues values = new ContentValues();
		putValues(values, issueCategory);
		return mDb.insert(TABLE_ISSUE_CATEGORIES, "", values);
	}

	public int update(final IssueCategory issueCategory) {
		final ContentValues values = new ContentValues();
		putValues(values, issueCategory);
		String selection = Util.join(new String[] {
				KEY_ID + " = " + issueCategory.id,
				KEY_PROJECT_ID + " = " + issueCategory.project.id,
				KEY_SERVER_ID + " = " + issueCategory.server.rowId
		}, " AND ");
		return mDb.update(TABLE_ISSUE_CATEGORIES, values, selection, null);
	}

	public Cursor selectCursor(final Server server, Project project, final long id, final String[] columns) {
		String selection = Util.join(new String[] {
				KEY_ID + " = " + id,
				KEY_PROJECT_ID + " = " + project.id,
				KEY_SERVER_ID + " = " + server.rowId
		}, " AND ");
		return mDb.query(TABLE_ISSUE_CATEGORIES, columns, selection, null, null, null, null);
	}

	public IssueCategory select(final Server server, Project project, final long rowId, final String[] columns) {
		final Cursor c = selectCursor(server, project, rowId, columns);
		IssueCategory issueCategory = null;
		if (c != null) {
			if (c.moveToFirst()) {
				issueCategory = new IssueCategory(server, project, c);
			}
			c.close();
		}

		return issueCategory;
	}

	public ArrayList<IssueCategory> selectAll(final Server server, final Project project) {
		String condition = KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_PROJECT_ID + " = " + project.id;
		final Cursor c = mDb.query(TABLE_ISSUE_CATEGORIES, null, condition, null, null, null, null);
		ArrayList<IssueCategory> categories = null;
		if (c != null) {
			if (c.moveToFirst()) {
				categories = new ArrayList<IssueCategory>();
				do {
					categories.add(new IssueCategory(server, project, c));
				} while (c.moveToNext());
			}
			c.close();
		}
		return categories;
	}

	/**
	 * Removes issues for a given project on a given server
	 */
	public int deleteAll(final Server server, long projectId) {
		String where = KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_PROJECT_ID + " = " + projectId;
		return mDb.delete(TABLE_ISSUE_CATEGORIES, where, null);
	}
}
