package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuePrioritiesDbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class Issue {
	public long id;
	// public Reference project;
	public Tracker tracker;
	public IssuePriority priority;
	public IssueStatus status;
	public User author;
	public String subject;
	public String description;
	public Calendar start_date;
	public int done_ratio;
	public Calendar created_on;
	public Calendar updated_on;
	public Calendar due_date;
	public Version fixed_version;
	public IssueCategory category;
	public Reference parent;
	public User assigned_to;
	public double estimated_hours;
	public double spent_hours;
	public List<Journal> journals;
	public List<ChangeSet> changesets;
	public List<Attachment> attachments;
	public boolean is_private;

	public Server server;
	public Project project;
	public boolean is_favorite;

	public Issue(Server server, Project project) {
		this.server = server;
		this.project = project;
		start_date = new GregorianCalendar();
		created_on = new GregorianCalendar();
		updated_on = new GregorianCalendar();
		due_date = new GregorianCalendar();
	}

	public Issue(final Server server, final Cursor c) {
		this.server = server;
		project = new Project(); // created early because some fields will hold a reference onto it
		construct(c);
	}

	private void construct(final Cursor c) {
		for (final String col : IssuesDbAdapter.ISSUE_FIELDS) {
			try {
				final int columnIndex = c.getColumnIndex(col);
				if (columnIndex < 0) {
					continue;
				}

				if (col.equals(IssuesDbAdapter.KEY_ID)) {
					id = c.getLong(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_PROJECT_ID)) {
					// TODO
					project.id = c.getInt(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_TRACKER_ID)) {
					tracker = new Tracker(server, c, col + "_");
				} else if (col.equals(IssuesDbAdapter.KEY_PRIORITY_ID)) {
					priority = new IssuePriority(server);
					priority.id = c.getLong(columnIndex);
					int pId = c.getColumnIndex(col + "_" + IssuePrioritiesDbAdapter.KEY_NAME);
					if (pId >= 0) {
						priority.name = c.getString(pId);
					}
				} else if (col.equals(IssuesDbAdapter.KEY_STATUS_ID)) {
					status = new IssueStatus();
					status.id = c.getLong(columnIndex);
					int sId = c.getColumnIndex(col + "_" + IssueStatusesDbAdapter.KEY_NAME);
					if (sId >= 0) {
						status.name = c.getString(sId);
					}
				} else if (col.equals(IssuesDbAdapter.KEY_AUTHOR_ID)) {
					author = new User(c, col + "_");
				} else if (col.equals(IssuesDbAdapter.KEY_ASSIGNED_TO_ID)) {
					assigned_to = new User(c, col + "_");
				} else if (col.equals(IssuesDbAdapter.KEY_SUBJECT)) {
					subject = c.getString(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_DESCRIPTION)) {
					description = c.getString(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_START_DATE)) {
					start_date = new GregorianCalendar();
					start_date.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_DONE_RATIO)) {
					done_ratio = c.getInt(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_CREATED_ON)) {
					created_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_UPDATED_ON)) {
					updated_on = new GregorianCalendar();
					updated_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_DUE_DATE)) {
					due_date = new GregorianCalendar();
					due_date.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_FIXED_VERSION_ID)) {
					fixed_version = new Version(c, col + "_");
				} else if (col.equals(IssuesDbAdapter.KEY_CATEGORY_ID)) {
					category = new IssueCategory(server, project);
					category.id = c.getLong(columnIndex);
					int cId = c.getColumnIndex(col + "_" + IssueCategoriesDbAdapter.KEY_NAME);
					if (cId > 0) {
						category.name = c.getString(cId);
					}
				} else if (col.equals(IssuesDbAdapter.KEY_PARENT_ID)) {
					parent = new Reference();
					parent.id = c.getLong(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ESTIMATED_HOURS)) {
					estimated_hours = c.getDouble(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_SPENT_HOURS)) {
					spent_hours = c.getDouble(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_IS_FAVORITE)) {
					is_favorite = c.getInt(columnIndex) > 0;
				} else if (col.equals(IssuesDbAdapter.KEY_IS_PRIVATE)) {
					is_private = c.getInt(columnIndex) > 0;
				} else if (col.equals(IssuesDbAdapter.KEY_SERVER_ID)) {
				} else {
					L.e("Unhandled column! " + col, null);
				}
			} catch (final Exception e) {
				L.e("Unhandled exception while creating an Issue: " + e, e);
			}
		}
	}

	public String toString() {
		return this.getClass().getSimpleName() + " {" + id + ", " +
				server + ", " +
				project + ", " +
				tracker + ", " +
				priority + ", " +
				status + ", " +
				author + ", " +
				subject + ", " +
				description + ", " +
				start_date + ", " +
				done_ratio + ", " +
				created_on + ", " +
				updated_on + ", " +
				due_date + ", " +
				fixed_version + ", " +
				category + ", " +
				parent + ", " +
				assigned_to + ", " +
				estimated_hours + ", " +
				spent_hours + ", " +
				journals + ", " +
				changesets + "}";
	}
}
