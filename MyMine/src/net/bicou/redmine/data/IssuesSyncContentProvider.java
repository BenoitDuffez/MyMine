package net.bicou.redmine.data;

import net.bicou.redmine.util.L;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class IssuesSyncContentProvider extends ContentProvider {
	@Override
	public int delete(final Uri uri, final String where, final String[] whereArgs) {
		L.d("");
		return 0;
	}

	@Override
	public String getType(final Uri uri) {
		L.d("");
		return null;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		L.d("");
		return null;
	}

	@Override
	public boolean onCreate() {
		L.d("");
		return false;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
		L.d("");
		return null;
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String where, final String[] whereArgs) {
		L.d("");
		return 0;
	}

}
