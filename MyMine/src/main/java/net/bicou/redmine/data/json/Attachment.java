package net.bicou.redmine.data.json;

import android.database.Cursor;

import com.google.gson.reflect.TypeToken;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.util.L;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by bicou on 16/07/13.
 */
public class Attachment {
	public long id;
	public String filename;
	public long filesize;
	public String content_type;
	public String description;
	public String content_url;
	public User author;
	public Calendar created_on;

	public static Type LIST_OF_ATTACHMENTS_TYPE = new TypeToken<List<Attachment>>() {}.getType();

	public Attachment(Server server, DbAdapter db, final Cursor c) {
		int colIndex;
		for (String col : IssuesDbAdapter.ATTACHMENT_FIELDS) {
			colIndex = c.getColumnIndex(col);
			try {
				if (col.equals(IssuesDbAdapter.KEY_ATTN_ID)) {
					id = c.getInt(colIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_FILENAME)) {
					filename = c.getString(colIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_FILESIZE)) {
					filesize = c.getLong(colIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_CONTENT_TYPE)) {
					content_type = c.getString(colIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_DESCRIPTION)) {
					description = c.getString(colIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_CONTENT_URL)) {
					content_url = c.getString(colIndex);
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_AUTHOR_ID)) {
					UsersDbAdapter udb = new UsersDbAdapter(db);
					author = udb.select(server, c.getInt(colIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_CREATED_ON)) {
					created_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(colIndex));
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_SERVER_ID)) {
				} else if (col.equals(IssuesDbAdapter.KEY_ATTN_ISSUE_ID)) {
				} else {
					L.e("Unparsed attachment column: " + col, null);
				}
			} catch (Exception e) {
				L.e("Couldn't parse column: " + col, e);
			}
		}
	}
}
