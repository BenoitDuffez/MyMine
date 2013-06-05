package net.bicou.redmine.platform;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import android.content.Context;

public class UsersManager {
	public static synchronized long updateUsers(final Context context, final Server server, final List<User> remoteList, final long lastSyncMarker) {
		final UsersDbAdapter db = new UsersDbAdapter(context);
		db.open();
		final List<User> localUsers = db.selectAll(server);

		long currentSyncMarker = lastSyncMarker;
		long userCreationDate;
		User localUser;
		final Calendar lastKnownChange = new GregorianCalendar();
		lastKnownChange.setTimeInMillis(lastSyncMarker);

		for (final User user : remoteList) {
			localUser = null;
			for (final User lu : localUsers) {
				if (lu != null && lu.id > 0 && user != null && user.id > 0 && lu.id == user.id) {
					localUser = lu;
					break;
				}
			}

			if (localUser == null) {
				// New, add it
				db.insert(server, user);
			} else {
				userCreationDate = user.created_on.getTimeInMillis();

				// Save current sync marker
				if (userCreationDate > currentSyncMarker) {
					currentSyncMarker = userCreationDate;
				}

				// Check status
				if (localUser.created_on.before(user.created_on)) {
					// Outdated, update it
					db.update(server, user);
				} else {
					// Up to date
				}
			}
		}
		db.close();

		return currentSyncMarker;
	}
}
