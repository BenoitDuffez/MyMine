package net.bicou.redmine.data.json;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Project implements Parcelable {
	public long id;
	public String name;
	public String description;
	public String identifier;
	public Reference parent;
	public Calendar created_on;
	public Calendar updated_on;
	public Server server;

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

	public Project(final Cursor c, final ProjectsDbAdapter db) {
		for (String col : ProjectsDbAdapter.PROJECT_FIELDS) {
			final int pos = col.indexOf(" ");
			if (pos > 0) {
				col = col.substring(0, pos);
			}
			try {
				if (col.equals(ProjectsDbAdapter.KEY_ID)) {
					id = c.getLong(c.getColumnIndex(ProjectsDbAdapter.KEY_ID));
				} else if (col.equals(ProjectsDbAdapter.KEY_NAME)) {
					name = c.getString(c.getColumnIndex(ProjectsDbAdapter.KEY_NAME));
				} else if (col.equals(ProjectsDbAdapter.KEY_DESCRIPTION)) {
					description = c.getString(c.getColumnIndex(ProjectsDbAdapter.KEY_DESCRIPTION));
				} else if (col.equals(ProjectsDbAdapter.KEY_IDENTIFIER)) {
					identifier = c.getString(c.getColumnIndex(ProjectsDbAdapter.KEY_IDENTIFIER));
				} else if (col.equals(ProjectsDbAdapter.KEY_CREATED_ON)) {
					created_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(c.getColumnIndex(ProjectsDbAdapter.KEY_CREATED_ON)));
				} else if (col.equals(ProjectsDbAdapter.KEY_UPDATED_ON)) {
					updated_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(c.getColumnIndex(ProjectsDbAdapter.KEY_UPDATED_ON)));
				} else if (col.equals(ProjectsDbAdapter.KEY_SERVER_ID)) {
					final long serverId = c.getLong(c.getColumnIndex(ProjectsDbAdapter.KEY_SERVER_ID));
					final ServersDbAdapter sdb = new ServersDbAdapter(db);
					server = sdb.getServer(serverId);
				} else if (col.equals(ProjectsDbAdapter.KEY_PARENT_ID)) {
					parent = new Reference();
					parent.id = c.getLong(c.getColumnIndex(ProjectsDbAdapter.KEY_PARENT_ID));
					if (parent.id != id && parent.id > 0) {
						final Project p = db.select(server, parent.id, new String[] {
							DbAdapter.KEY_REFERENCE_NAME
						});
						if (p != null) {
							parent.name = p.name;
						} else {
							parent.name = "";
						}
					} else {
						parent.name = "";
					}
				} else {
					L.e("Unhandled column: " + col, null);
				}
			} catch (final Exception e) {
			}
		}
	}
}
