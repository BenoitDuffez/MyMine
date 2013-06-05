package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.*;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class Issue {
	public long id;
	// public Reference project;
	public Reference tracker;
	public Reference priority;
	public Reference status;
	public Reference author;
	public String subject;
	public String description;
	public Calendar start_date;
	public int done_ratio;
	public Calendar created_on;
	public Calendar updated_on;
	public Calendar due_date;
	public Reference fixed_version;
	public Reference category;
	public Reference parent;
	public Reference assigned_to;
	public double estimated_hours;
	public double spent_hours;
	public List<Journal> journals;
	public List<ChangeSet> changesets;

	public Server server;
	public Project project;

	public Issue(final Server server, final Cursor c, final IssuesDbAdapter db) {
		this.server = server;
		construct(c, db);
	}

	private void construct(final Cursor c, final IssuesDbAdapter db) {
		for (final String col : IssuesDbAdapter.ISSUE_FIELDS) {
			try {
				final int columnIndex = c.getColumnIndex(col);
				if (columnIndex < 0) {
					continue;
				}
				if (col.equals(IssuesDbAdapter.KEY_ID)) {
					id = c.getLong(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_PROJECT_ID)) {
					final ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);
					project = pdb.select(server, c.getLong(columnIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_TRACKER_ID)) {
					// TODO
				} else if (col.equals(IssuesDbAdapter.KEY_PRIORITY_ID)) {
					// TODO
				} else if (col.equals(IssuesDbAdapter.KEY_STATUS_ID)) {
					final IssueStatusesDbAdapter isdb = new IssueStatusesDbAdapter(db);
					status = new Reference();
					status.id = c.getLong(columnIndex);
					status.name = isdb.getName(server, status.id);
				} else if (col.equals(IssuesDbAdapter.KEY_AUTHOR_ID)) {
					// TODO
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
					final VersionsDbAdapter vdb = new VersionsDbAdapter(db);
					fixed_version = new Reference();
					fixed_version.id = c.getLong(columnIndex);
					fixed_version.name = vdb.getName(server, project, fixed_version.id);
				} else if (col.equals(IssuesDbAdapter.KEY_CATEGORY_ID)) {
					// TODO
				} else if (col.equals(IssuesDbAdapter.KEY_PARENT_ID)) {
					parent = new Reference();
					parent.id = c.getLong(columnIndex);
					parent.name = db.getName(server, parent.id);
				} else if (col.equals(IssuesDbAdapter.KEY_ASSIGNED_TO_ID)) {
					// TODO
				} else if (col.equals(IssuesDbAdapter.KEY_ESTIMATED_HOURS)) {
					estimated_hours = c.getDouble(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_SPENT_HOURS)) {
					spent_hours = c.getDouble(columnIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_SERVER_ID)) {
					final ServersDbAdapter sdb = new ServersDbAdapter(db);
					server = sdb.getServer(c.getLong(columnIndex));
				} else {
					L.e("Unhandled column! " + col, null);
				}
			} catch (final Exception e) {
				L.e("Unhandled exception while creating an Issue: " + e, e);
			}
		}
	}
}
