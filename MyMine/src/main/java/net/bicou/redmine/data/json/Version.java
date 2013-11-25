package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class Version {
	public enum VersionStatus {
		OPEN,
		LOCKED,
		CLOSED,
		INVALID,
	}

	public long id;
	public Project project;
	public String name;
	public String description;
	public VersionStatus status;
	public Calendar due_date;
	public String sharing;
	public List<CustomField> custom_fields;
	public Calendar created_on;
	public Calendar updated_on;

	public Version() {
	}

	public Version(final Cursor c) {
		this(c, "");
	}

	public Version(Cursor c, String columnPrefix) {
		int columnIndex;
		for (final String col : VersionsDbAdapter.VERSION_FIELDS) {
			columnIndex = c.getColumnIndex(columnPrefix + col);
			if (columnIndex < 0) {
				continue;
			}
			try {
				if (col.equals(VersionsDbAdapter.KEY_CREATED_ON)) {
					created_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(VersionsDbAdapter.KEY_UPDATED_ON)) {
					updated_on = new GregorianCalendar();
					updated_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(VersionsDbAdapter.KEY_DUE_DATE)) {
					due_date = new GregorianCalendar();
					due_date.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(VersionsDbAdapter.KEY_DESCRIPTION)) {
					description = c.getString(columnIndex);
				} else if (col.equals(VersionsDbAdapter.KEY_NAME)) {
					name = c.getString(columnIndex);
				} else if (col.equals(VersionsDbAdapter.KEY_SHARING)) {
					sharing = c.getString(columnIndex);
				} else if (col.equals(VersionsDbAdapter.KEY_STATUS)) {
					status = VersionStatus.valueOf(c.getString(columnIndex).toUpperCase(Locale.ENGLISH));
				} else if (col.equals(VersionsDbAdapter.KEY_SERVER_ID)) {
					// No op
				} else if (col.equals(VersionsDbAdapter.KEY_PROJECT_ID)) {
					project = new Project(c, ProjectsDbAdapter.TABLE_PROJECTS + "_");
				} else if (col.equals(VersionsDbAdapter.KEY_ID)) {
					id = c.getInt(columnIndex);
				} else {
					L.e("Unhandled " + getClass().getSimpleName() + " column: " + col + " prefix: " + columnPrefix, null);
				}
			} catch (Exception e) {
				L.e("Can't parse " + getClass().getSimpleName() + " column: " + col + " prefix: " + columnPrefix, e);
			}
		}
	}

	@Override
	public String toString() {
		return super.toString() + " " + getClass().getSimpleName() + " { name: " + name + ", description: " + description + ", status: " + status + ", " +
				"sharing: " + sharing + ", create/updated/due dates: " + (Util.isEpoch(created_on) ? "epoch" : created_on.getTime()) + "/" + (Util.isEpoch
				(updated_on) ? "epoch" : updated_on.getTime()) + "/" + (Util.isEpoch(due_date) ? "epoch" : due_date.getTime()) + " }";
	}
}
