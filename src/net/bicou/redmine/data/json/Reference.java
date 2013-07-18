package net.bicou.redmine.data.json;


import android.database.Cursor;
import net.bicou.redmine.util.L;

public class Reference {
	public String name;
	public long id;

	public static final String KEY_NAME = "name";
	public static final String KEY_ID = "id";

	public Reference() {
	}

	public Reference(Cursor c) {
		try {
			name = c.getString(c.getColumnIndex(KEY_NAME));
			id = c.getLong(c.getColumnIndex(KEY_ID));
		} catch (Exception e) {
			L.e("Couldn't get name/id from Reference", e);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " { id: " + id + ", name: " + name + " }";
	}

	public boolean isColumnHandled(final String col) {
		return KEY_NAME.equals(col) || KEY_ID.equals(col);
	}
}
