package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 21/05/13.
 */
public class IssueCategory extends Reference {
	public Project project;
	public Reference assigned_to;

	public Server server;

	public IssueCategory(Server server, Project project) {
		this.server = server;
		this.project = project;
	}

	public IssueCategory(Server server, Project project, Cursor cursor) {
		this(server, project);

		int columnIndex;
		for (String col : IssueCategoriesDbAdapter.ISSUE_CATEGORY_FIELDS) {
			try {
				columnIndex = cursor.getColumnIndex(col);
				if (IssueCategoriesDbAdapter.KEY_ID.equals(col)) {
					id = cursor.getInt(columnIndex);
				} else if (IssueCategoriesDbAdapter.KEY_ASSIGNED_TO_ID.equals(col)) {
					assigned_to = new Reference();
					assigned_to.id = cursor.getLong(columnIndex);
				} else if (IssueCategoriesDbAdapter.KEY_NAME.equals(col)) {
					name = cursor.getString(columnIndex);
				} else if (IssueCategoriesDbAdapter.KEY_PROJECT_ID.equals(col)) {
				} else if (IssueCategoriesDbAdapter.KEY_SERVER_ID.equals(col)) {
				} else {
					L.e("Unhandled IssueCategory column: " + col, null);
				}
			} catch (Exception e) {
				L.e("Can't parse column: " + col, e);
			}
		}
	}
}
