package net.bicou.redmine.data.json;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class Project implements Parcelable {
	public long id;
	public String name;
	public String description;
	public String identifier;
	public Reference parent;
	public Calendar created_on;
	public Calendar updated_on;

	public List<Tracker> trackers;
	public List<IssueCategory> issue_categories;

	public Server server;
	public boolean is_favorite;
	public boolean is_sync_blocked;

	protected Project(final Parcel in) {
		id = in.readLong();
		name = in.readString();
		description = in.readString();
		identifier = in.readString();
		final long id = in.readLong();
		if (id > 0) {
			parent = new Reference();
			parent.id = id;
			parent.name = in.readString();
		} else {
			parent = null;
			in.readString();
		}
		server = in.readParcelable(Server.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeLong(id);
		dest.writeString(name);
		dest.writeString(description);
		dest.writeString(identifier);
		dest.writeLong(parent == null ? -1 : parent.id);
		dest.writeString(parent == null ? null : parent.name);
		dest.writeParcelable(server, flags);
	}

	public static final Parcelable.Creator<Project> CREATOR = new Parcelable.Creator<Project>() {
		@Override
		public Project createFromParcel(final Parcel in) {
			return new Project(in);
		}

		@Override
		public Project[] newArray(final int size) {
			return new Project[size];
		}
	};

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " { id: " + id + ", name: " + name + ", identifier: " + identifier + " }";
	}

	public Project() {
	}

	public Project(final Cursor c) {
		this(c, "");
	}

	public Project(final Cursor c, String columnPrefix) {
		for (String col : ProjectsDbAdapter.PROJECT_FIELDS) {
			final int pos = col.indexOf(" ");
			if (pos > 0) {
				col = col.substring(0, pos);
			}
			try {
				int columnIndex = c.getColumnIndex(columnPrefix + col);
				if (columnIndex < 0) {
					continue;
				}

				if (col.equals(ProjectsDbAdapter.KEY_ID)) {
					id = c.getLong(columnIndex);
				} else if (col.equals(ProjectsDbAdapter.KEY_NAME)) {
					name = c.getString(columnIndex);
				} else if (col.equals(ProjectsDbAdapter.KEY_DESCRIPTION)) {
					description = c.getString(columnIndex);
				} else if (col.equals(ProjectsDbAdapter.KEY_IDENTIFIER)) {
					identifier = c.getString(columnIndex);
				} else if (col.equals(ProjectsDbAdapter.KEY_CREATED_ON)) {
					created_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(ProjectsDbAdapter.KEY_UPDATED_ON)) {
					updated_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(ProjectsDbAdapter.KEY_SERVER_ID)) {
					server = new Server(c, ServersDbAdapter.TABLE_SERVERS + "_");
				} else if (col.equals(ProjectsDbAdapter.KEY_PARENT_ID)) {
					parent = new Reference();
					parent.id = c.getLong(columnIndex);
					// We can't get the full project info otherwise it'd break the current cursor we're reading
					parent.name = null;
				} else if (col.equals(ProjectsDbAdapter.KEY_IS_FAVORITE)) {
					is_favorite = c.getInt(columnIndex) > 0;
				} else if (col.equals(ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED)) {
					is_sync_blocked = c.getInt(columnIndex) > 0;
				} else {
					L.e("Unhandled column: " + col, null);
				}
			} catch (final Exception e) {
				L.e("Couldn't parse project column: " + col, e);
			}
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (1 << 0) * (server == null ? 0 : 1);
		hash += (1 << 1) * (server == null ? 0 : server.rowId);
		hash += (1 << 16) * (id);
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		return other instanceof Project && hashCode() == other.hashCode();
	}
}
