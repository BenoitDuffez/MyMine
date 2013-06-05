package net.bicou.redmine.data.json;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import android.database.Cursor;

public class Version {
	public enum VersionStatus {
		OPEN,
		LOCKED,
		CLOSED,
		INVALID,
	};

	public int id;
	public Reference project;
	public String name;
	public String description;
	public VersionStatus status;
	public Calendar due_date;
	public String sharing;
	public List<CustomField> custom_fields;
	public Calendar created_on;
	public Calendar updated_on;

	public Version(final Cursor c, final DbAdapter db) {
		int columnIndex;
		for (final String col : VersionsDbAdapter.VERSION_FIELDS) {
			columnIndex = c.getColumnIndex(col);
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
			} else if (col.equals(VersionsDbAdapter.KEY_ID)) {
				id = c.getInt(columnIndex);
			}
		}
	}
}
