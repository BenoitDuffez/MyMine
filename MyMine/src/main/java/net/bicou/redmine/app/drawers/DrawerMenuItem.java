package net.bicou.redmine.app.drawers;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by bicou on 09/08/13.
 */
public abstract class DrawerMenuItem<T extends Enum> {
	private final T mViewType;

	public DrawerMenuItem(T viewType) {
		mViewType = viewType;
	}

	public abstract View fillView(View convertView, ViewGroup parent);

	public abstract long getId();

	public abstract DrawerMenuItem setTag(Object tag);

	public abstract Object getTag();

	public int getViewTypeCount() {
		return mViewType.getClass().getFields().length;
	}

	public int getViewType() {
		return mViewType.ordinal();
	}
}
