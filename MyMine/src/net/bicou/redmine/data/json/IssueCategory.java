package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 21/05/13.
 */
public class IssueCategory extends Reference {
	public Project project;
	public User assigned_to;

	public Server server;

	public IssueCategory(Server server, Project project, Cursor cursor, DbAdapter db) {
		this.server = server;
		this.project = project;
		int columnIndex;
		for (String col : IssueCategoriesDbAdapter.ISSUE_CATEGORY_FIELDS) {
			try {
				columnIndex = cursor.getColumnIndex(col);
				if (IssueCategoriesDbAdapter.KEY_ID.equals(col)) {
					id = cursor.getInt(columnIndex);
				} else if (IssueCategoriesDbAdapter.KEY_ASSIGNED_TO_ID.equals(col)) {
					UsersDbAdapter udb = new UsersDbAdapter(db);
					assigned_to = udb.select(server, cursor.getInt(columnIndex));
				} else {
					L.e("Unhandled IssueCategory column: " + col, null);
				}
			} catch (Exception e) {
				L.e("Can't parse column: " + col, e);
			}
		}
	}
}
