package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;

public class UsersDbAdapter extends DbAdapter {
	public static final String TABLE_USERS = "users";

	public static final String KEY_ID = "id";
	public static final String KEY_FIRSTNAME = "firstname";
	public static final String KEY_LASTNAME = "lastname";
	public static final String KEY_MAIL = "mail";
	public static final String KEY_CREATED_ON = "created_on";
	public static final String KEY_LAST_LOGIN_ON = "last_login_on";

	public static final String KEY_SERVER_ID = "server_id";

	public static final String[] USER_FIELDS = new String[] {
			KEY_ID,
			KEY_FIRSTNAME,
			KEY_LASTNAME,
			KEY_MAIL,
			KEY_CREATED_ON,
			KEY_LAST_LOGIN_ON,

			KEY_SERVER_ID,
	};

	/**
	 * Table creation statements
	 */
	public static final String[] getCreateTablesStatements() {
		return new String[] {
				"CREATE TABLE " + TABLE_USERS + "(" + Util.join(USER_FIELDS, ", ") + ", PRIMARY KEY (" + KEY_ID + ", " + KEY_SERVER_ID + "))",
		};
	}

	public UsersDbAdapter(final Context ctx) {
		super(ctx);
	}

	public UsersDbAdapter(final DbAdapter db) {
		super(db);
	}

	public static String getFieldAlias(String table, String field) {
		return table + "." + field + " AS " + table + "_" + field;
	}

	private void putValues(final Server server, final ContentValues values, final User user) {
		values.put(KEY_ID, user.id);
		values.put(KEY_FIRSTNAME, user.firstname);
		values.put(KEY_LASTNAME, user.lastname);
		values.put(KEY_MAIL, user.mail);
		values.put(KEY_CREATED_ON, user.created_on == null ? 0 : user.created_on.getTimeInMillis());
		values.put(KEY_LAST_LOGIN_ON, user.last_login_on == null ? 0 : user.last_login_on.getTimeInMillis());
		values.put(KEY_SERVER_ID, server.rowId);
	}

	public long insert(final Server server, final User user) {
		final ContentValues values = new ContentValues();
		putValues(server, values, user);
		return mDb.insert(TABLE_USERS, "", values);
	}

	public int update(final Server server, final User user) {
		final ContentValues values = new ContentValues();
		putValues(server, values, user);
		return mDb.update(TABLE_USERS, values, KEY_ID + " = " + user.id + " AND " + KEY_SERVER_ID + " = " + server.rowId, null);
	}

	public int deleteAll(final Server server) {
		final String where = KEY_SERVER_ID + " = " + server.rowId;
		return mDb.delete(TABLE_USERS, where, null);
	}

	public User select(final Server server, final long userId) {
		final Cursor c = mDb.query(TABLE_USERS, null, KEY_SERVER_ID + " = " + server.rowId + " AND " + KEY_ID + " = " + userId, null, null, null, null, null);
		User user = null;
		if (c != null) {
			if (c.moveToFirst()) {
				user = new User(c);
			}
			c.close();
		}
		return user;
	}

	public List<User> selectAll(final Server server) {
		final String selection = server != null && server.rowId > 0 ? KEY_SERVER_ID + " = " + server.rowId : null;
		final Cursor c = mDb.query(TABLE_USERS, USER_FIELDS, selection, null, null, null, null);
		final List<User> list = new ArrayList<User>();

		if (c != null) {
			while (c.moveToNext()) {
				list.add(new User(c));
			}
			c.close();
		}

		return list;
	}
}
