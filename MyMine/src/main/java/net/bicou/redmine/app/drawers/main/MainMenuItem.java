package net.bicou.redmine.app.drawers.main;

import net.bicou.redmine.app.drawers.DrawerMenuItem;

/**
 * Created by bicou on 19/07/13.
 */
public abstract class MainMenuItem<T extends Enum> extends DrawerMenuItem<T> {
	protected DrawerMenuFragment drawerMenuFragment;
	private Object mData;

	public MainMenuItem(final DrawerMenuFragment drawerMenuFragment, T viewType) {
		super(viewType);
		this.drawerMenuFragment = drawerMenuFragment;
	}

	@Override
	public Object getTag() {
		return mData;
	}

	@Override
	public MainMenuItem<T> setTag(final Object data) {
		this.mData = data;
		return this;
	}

	public long getId() {
		return 0;
	}
}
