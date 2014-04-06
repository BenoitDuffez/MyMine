package net.bicou.redmine.app.issues.issue;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Adapter for listview-based issue fragment tabs
 */
abstract class IssueItemsAdapter<ItemType> extends BaseAdapter {
	private final List<ItemType> mItems;
	Context mContext;
	java.text.DateFormat mDateFormat;
	java.text.DateFormat mTimeFormat;

	static class JournalViewsHolder {
		TextView user, date, details, notes;
		ImageView avatar;
	}

	public IssueItemsAdapter(final Context context, List<ItemType> attachments) {
		mItems = attachments;
		mContext = context;
		if (mContext != null) {
			mDateFormat = DateFormat.getLongDateFormat(mContext);
			mTimeFormat = DateFormat.getTimeFormat(mContext);
		}
	}

	public int getCount() {
		if (mItems != null) {
			return mItems.size();
		}
		return 0;
	}

	public ItemType getItem(int position) {
		if (mItems != null) {
			return mItems.get(position);
		}
		return null;
	}

	public long getItemId(int position) {
		ItemType item = mItems.get(position);
		if (item == null) {
			return 0;
		}
		try {
			Field id = item.getClass().getField("id");
			if (id == null) {
				return 0;
			}
			return (Long) id.get(item);
		} catch (NoSuchFieldException e) {
			// No failure needed
		} catch (IllegalAccessException e) {
			L.e("Shouldn't happen", e);
		}
		return 0;
	}

	protected abstract void bindData(JournalViewsHolder holder, ItemType item);

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		if (mContext == null) {
			return null;
		}

		final JournalViewsHolder holder;
		if (convertView == null) {
			final LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.issue_journal_list_item, parent, false);

			holder = new JournalViewsHolder();
			holder.user = (TextView) convertView.findViewById(R.id.issue_journal_item_user);
			holder.date = (TextView) convertView.findViewById(R.id.issue_journal_item_date);
			holder.details = (TextView) convertView.findViewById(R.id.issue_journal_item_details);
			holder.notes = (TextView) convertView.findViewById(R.id.issue_journal_item_notes);
			holder.avatar = (ImageView) convertView.findViewById(R.id.issue_journal_item_avatar);

			convertView.setTag(holder);
		} else {
			holder = (JournalViewsHolder) convertView.getTag();
		}

		ItemType item = getItem(position);
		if (item != null) {
			bindData(holder, item);
		}

		return convertView;
	}
}
