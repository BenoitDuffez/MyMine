package net.bicou.redmine.app.wiki;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.bicou.redmine.R;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;

import java.text.DateFormat;
import java.util.Date;

/**
 * CursorAdapter that will map Cursor data to a layout
 *
 * @author bicou
 */
public final class WikiPagesCursorAdapter extends CursorAdapter {
	private final Context mContext;

	public class ViewHolder {
		TextView pageName, updatedOn;
	}

	public WikiPagesCursorAdapter(final Context context, final Cursor c, final boolean autoRequery) {
		super(context, c, autoRequery);
		mContext = context;
	}

	@Override
	public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
		final View view = LayoutInflater.from(mContext).inflate(R.layout.wikipage_listitem, parent, false);
		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.pageName = (TextView) view.findViewById(R.id.wiki_item_name);
		viewHolder.updatedOn = (TextView) view.findViewById(R.id.wiki_item_updated_on);
		view.setTag(viewHolder);
		return view;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		final ViewHolder viewHolder = (ViewHolder) view.getTag();
		viewHolder.pageName.setText(cursor.getString(cursor.getColumnIndex(WikiDbAdapter.KEY_TITLE)));
		final Date date = new Date(cursor.getLong(cursor.getColumnIndex(WikiDbAdapter.KEY_UPDATED_ON)));
		final String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(date);
		final String updatedOn = context.getString(R.string.project_list_item_updated_on, formattedDate);
		viewHolder.updatedOn.setText(updatedOn);
	}
}
