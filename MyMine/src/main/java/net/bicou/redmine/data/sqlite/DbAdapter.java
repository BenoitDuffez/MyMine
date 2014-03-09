package net.bicou.redmine.data.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import net.bicou.redmine.data.json.Reference;
import net.bicou.redmine.util.L;

public abstract class DbAdapter {
	Context mContext;
	protected DbManager mDbManager;
	SQLiteDatabase mDb;

	public static final String KEY_ROWID = "_id";
	public static final String KEY_REFERENCE_NAME = Reference.KEY_NAME;

	public DbAdapter(final Context ctx) {
		mContext = ctx;
	}

	public DbAdapter(final DbAdapter other) {
		mContext = other.mContext;
		mDb = other.mDb;
		mDbManager = other.mDbManager;
	}


	public synchronized DbAdapter open() throws SQLException {
		if (mDb != null) {
			return this;
		}

		mDbManager = new DbManager(mContext);
		try {
			mDb = mDbManager.getWritableDatabase();
		} catch (final SQLException e) {
			L.e("Unable to open DB, trying again in 1 second", e);
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e1) {
				L.e("Could not wait 1 second " + e1);
			}
			mDb = mDbManager.getWritableDatabase();// This may crash
		}

		return this;
	}

	public synchronized void close() {
		mDbManager.close();
		mDbManager = null;
		mDb = null;
	}

	public Cursor rawQuery(String sql, String[] selArgs) {
		return mDb.rawQuery(sql, selArgs);
	}
}
