package net.bicou.redmine.data.json;

import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.TrackersDbAdapter;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 20/05/13.
 */
public class Tracker extends Reference {
	public Server server;

	public Tracker(Server s, Cursor c) {
		this(s, c, "");
	}

	public Tracker(Server s, Cursor c, String columnPrefix) {
		server = s;
		for (String col : TrackersDbAdapter.TRACKER_FIELDS) {
			try {
				int columnIndex = c.getColumnIndex(columnPrefix + col);
				if (columnIndex < 0) {
					continue;
				}

				if (TrackersDbAdapter.KEY_ID.equals(col)) {
					id = c.getInt(columnIndex);
				} else if (TrackersDbAdapter.KEY_SERVER_ID.equals(col)) {
				} else if (TrackersDbAdapter.KEY_NAME.equals(col)) {
					name = c.getString(columnIndex);
				} else {
					L.e("Unhandled column for Tracker: " + col, null);
				}
			} catch (Exception e) {
				L.e("Unable to parse column " + col + " from cursor", e);
			}
		}
	}
}
