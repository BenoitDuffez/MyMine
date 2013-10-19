package net.bicou.redmine.app.drawers;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by bicou on 14/06/13.
 */
public class DrawerMenuItemsAdapter<T extends Enum> extends BaseAdapter {
	List<DrawerMenuItem<T>> mData;
	Context mContext;

	public DrawerMenuItemsAdapter(Context ctx, List<DrawerMenuItem<T>> data) {
		mContext = ctx;
		mData = data;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int i) {
		return getItem(i) != null;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver dataSetObserver) {
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
	}

	@Override
	public int getCount() {
		return mData == null ? 0 : mData.size();
	}

	@Override
	public DrawerMenuItem getItem(int i) {
		return mData == null ? null : mData.get(i);
	}

	@Override
	public long getItemId(int i) {
		return getItem(i) == null ? 0 : getItem(i).getId();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int i, View convertView, ViewGroup viewGroup) {
		DrawerMenuItem item = getItem(i);
		if (item == null) {
			return convertView;
		}
		return item.fillView(convertView, viewGroup);
	}

	@Override
	public int getItemViewType(int i) {
		return getItem(i) == null ? 0 : getItem(i).getViewType();
	}

	@Override
	public int getViewTypeCount() {
		for (int i = 0; i < getCount(); i++) {
			if (getItem(i) != null) {
				return getItem(i).getViewTypeCount();
			}
		}
		throw new IllegalStateException("This menu is invalid.");
	}

	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}
}
