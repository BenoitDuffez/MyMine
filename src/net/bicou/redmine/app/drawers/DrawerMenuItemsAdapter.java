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
public class DrawerMenuItemsAdapter extends BaseAdapter {
	public interface DrawerMenuItem {
		public View fillView(View convertView, ViewGroup parent);

		public long getId();

		public int getViewType();

		public int getTextId();
	}

	List<DrawerMenuItem> mData;
	Context mContext;

	public DrawerMenuItemsAdapter(Context ctx, List<DrawerMenuItem> data) {
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
		return 2;
	}

	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}
}
