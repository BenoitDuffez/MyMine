package net.bicou.redmine.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.sync.NetworkUtilities;
import net.bicou.redmine.sync.SyncUtils;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by bicou on 15/07/13.
 */
public class DbManager extends SQLiteOpenHelper {
	private static final String DB_FILE = "redmine.db";
	private static final int DB_VERSION = 16;
	Context mContext;
	Lock mLock = new ReentrantLock();

	public DbManager(final Context context) {
		super(context, DB_FILE, null, DB_VERSION);
		mContext = context;
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		mLock.lock();
		return super.getWritableDatabase();
	}

	@Override
	public void close() {
		super.close();
		mLock.unlock();
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		createTables(db, ServersDbAdapter.getCreateTablesStatements());
		createTables(db, ProjectsDbAdapter.getCreateTablesStatements());
		createTables(db, IssuesDbAdapter.getCreateTablesStatements());
		createTables(db, IssueStatusesDbAdapter.getCreateTablesStatements());
		createTables(db, VersionsDbAdapter.getCreateTablesStatements());
		createTables(db, WikiDbAdapter.getCreateTablesStatements());
		createTables(db, UsersDbAdapter.getCreateTablesStatements());
		createTables(db, QueriesDbAdapter.getCreateTablesStatements());
		createTables(db, TrackersDbAdapter.getCreateTablesStatements());
		createTables(db, IssueCategoriesDbAdapter.getCreateTablesStatements());
		createTables(db, IssuePrioritiesDbAdapter.getCreateTablesStatements());
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		L.d("oldVersion: " + oldVersion + ", newVersion: " + newVersion);

		if (oldVersion < 3) {
			try {
				db.execSQL("DROP TABLE " + ProjectsDbAdapter.TABLE_PROJECTS);
			} catch (final Exception e) {
			}
			createTables(db, ProjectsDbAdapter.getCreateTablesStatements());
		}

		if (oldVersion < 4) {
			try {
				db.execSQL("ALTER TABLE " + ProjectsDbAdapter.TABLE_PROJECTS + " ADD " + ProjectsDbAdapter.KEY_SERVER_ID + " integer");
			} catch (final Exception e) {
				L.e("Unable to add field " + ProjectsDbAdapter.KEY_SERVER_ID + ": " + e);
			}

			try {
				db.execSQL("UPDATE " + ProjectsDbAdapter.TABLE_PROJECTS + " SET " + ProjectsDbAdapter.KEY_SERVER_ID + " = (SELECT MIN(" + DbAdapter.KEY_ROWID +
						")" +
						" " +
						"FROM " + ServersDbAdapter.TABLE_SERVERS + ")");
			} catch (final Exception e) {
				L.e("Unable to set field " + ProjectsDbAdapter.KEY_SERVER_ID + ": " + e);
			}
		}

		// Rename "id" to KEY_ROWID
		if (oldVersion < 5) {
			// Create transaction
			db.beginTransaction();
			try {

				// First rename the old table
				db.execSQL("ALTER TABLE " + ProjectsDbAdapter.TABLE_PROJECTS + " RENAME TO tmp");

				// Then create the new table, based on the old table but
				// with the updated column name:
				createTables(db, ProjectsDbAdapter.getCreateTablesStatements());

				// Then copy the contents across from the original table.
				String sql = "INSERT INTO " + ProjectsDbAdapter.TABLE_PROJECTS;
				sql += " (" + Util.joinFirstWords(ProjectsDbAdapter.PROJECT_FIELDS, ", ") + ")";
				sql += " SELECT " + Util.joinFirstWords(ProjectsDbAdapter.PROJECT_FIELDS, ", ").replace(DbAdapter.KEY_ROWID + ",", "id,");
				sql += " FROM tmp";
				db.execSQL(sql);

				// Lastly, drop the old table & release the transaction
				db.execSQL("DROP TABLE tmp");
				db.setTransactionSuccessful();
			} catch (final Exception e) {
				L.e("Unable to upgrade DB from " + oldVersion + " to " + newVersion, e);
			} finally {
				db.endTransaction();
			}
		}

		if (oldVersion < 6) {
			db.execSQL("DROP TABLE " + ProjectsDbAdapter.TABLE_PROJECTS);
			createTables(db, ProjectsDbAdapter.getCreateTablesStatements());
			createTables(db, IssuesDbAdapter.getCreateTablesStatements());
		}

		if (oldVersion < 7) {
			try {
				db.execSQL("DROP TABLE " + ProjectsDbAdapter.TABLE_PROJECTS);
				db.execSQL("DROP TABLE " + "redmine_servers");
				db.execSQL("DROP TABLE " + IssuesDbAdapter.TABLE_ISSUES);
			} catch (final Exception e) {
				L.e(e.toString());
			}

			onCreate(db);
		}

		if (oldVersion < 8) {
			try {
				db.execSQL("DROP TABLE " + ProjectsDbAdapter.TABLE_PROJECTS);
				db.execSQL("DROP TABLE " + IssuesDbAdapter.TABLE_ISSUES);
				db.execSQL("DROP TABLE " + IssueStatusesDbAdapter.TABLE_ISSUE_STATUSES);
				db.execSQL("DROP TABLE " + VersionsDbAdapter.TABLE_VERSIONS);
				db.execSQL("ALTER TABLE redmine_servers RENAME TO " + ServersDbAdapter.TABLE_SERVERS);
			} catch (final Exception e) {
				L.e(e.toString());
			}

			createTables(db, ProjectsDbAdapter.getCreateTablesStatements());
			createTables(db, IssuesDbAdapter.getCreateTablesStatements());
			createTables(db, IssueStatusesDbAdapter.getCreateTablesStatements());
			createTables(db, VersionsDbAdapter.getCreateTablesStatements());
			createTables(db, WikiDbAdapter.getCreateTablesStatements());
			createTables(db, UsersDbAdapter.getCreateTablesStatements());

			final List<Server> servers = new ArrayList<Server>();
			final Cursor c = db.query(ServersDbAdapter.TABLE_SERVERS, null, null, null, null, null, null);
			if (c != null) {
				while (c.moveToNext()) {
					servers.add(new Server(c, db));
				}
			}

			db.execSQL("ALTER TABLE " + ServersDbAdapter.TABLE_SERVERS + " ADD " + ServersDbAdapter.KEY_USER_ID);

			for (final Server server : servers) {
				final User myself = NetworkUtilities.whoAmI(mContext, server);
				if (myself != null) {
					server.user = myself;
					final ContentValues values = new ContentValues();
					values.put(ServersDbAdapter.KEY_SERVER_URL, server.serverUrl);
					values.put(ServersDbAdapter.KEY_API_KEY, server.apiKey);
					values.put(ServersDbAdapter.KEY_USER_ID, server.user == null ? 0 : server.user.id);
					db.update(ServersDbAdapter.TABLE_SERVERS, values, DbAdapter.KEY_ROWID + " = " + server.rowId, null);

					values.clear();
					values.put(UsersDbAdapter.KEY_ID, myself.id);
					values.put(UsersDbAdapter.KEY_FIRSTNAME, myself.firstname);
					values.put(UsersDbAdapter.KEY_LASTNAME, myself.lastname);
					values.put(UsersDbAdapter.KEY_MAIL, myself.mail);
					values.put(UsersDbAdapter.KEY_CREATED_ON, myself.created_on == null ? 0 : myself.created_on.getTimeInMillis());
					values.put(UsersDbAdapter.KEY_LAST_LOGIN_ON, myself.last_login_on == null ? 0 : myself.last_login_on.getTimeInMillis());
					values.put(UsersDbAdapter.KEY_SERVER_ID, server.rowId);
					db.insert(UsersDbAdapter.TABLE_USERS, null, values);
				}
			}

			SyncUtils.enableSync(mContext);
		}

		if (oldVersion < 9) {
			db.execSQL("ALTER TABLE " + ServersDbAdapter.TABLE_SERVERS + " ADD " + ServersDbAdapter.KEY_AUTH_USERNAME);
			db.execSQL("ALTER TABLE " + ServersDbAdapter.TABLE_SERVERS + " ADD " + ServersDbAdapter.KEY_AUTH_PASSWORD);
		}

		if (oldVersion < 10) {
			createTables(db, QueriesDbAdapter.getCreateTablesStatements());
		}

		if (oldVersion < 11) {
			db.execSQL("ALTER TABLE " + IssueStatusesDbAdapter.TABLE_ISSUE_STATUSES + " ADD " + IssueStatusesDbAdapter.KEY_IS_CLOSED);
		}

		if (oldVersion < 12) {
			createTables(db, TrackersDbAdapter.getCreateTablesStatements());
		}

		if (oldVersion < 13) {
			createTables(db, IssueCategoriesDbAdapter.getCreateTablesStatements());
		}

		if (oldVersion < 14) {
			createTables(db, IssuePrioritiesDbAdapter.getCreateTablesStatements());
		}

		if (oldVersion < 15) {
			String[] attn = {
					IssuesDbAdapter.getCreateTablesStatements()[1],
			};
			createTables(db, attn);
		}

		if (oldVersion < 16) {
			db.execSQL("ALTER TABLE " + ProjectsDbAdapter.TABLE_PROJECTS + " ADD " + ProjectsDbAdapter.KEY_IS_FAVORITE);
			db.execSQL("DROP TABLE " + WikiDbAdapter.TABLE_WIKI);
			createTables(db, WikiDbAdapter.getCreateTablesStatements());
		}
	}

	private void createTables(final SQLiteDatabase db, final String[] statements) {
		for (final String sql : statements) {
			try {
				db.execSQL(sql);
			} catch (final Exception e) {
				L.e("Unable to create table: " + sql, e);
			}
		}
	}
}
