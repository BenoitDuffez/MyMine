package net.bicou.redmine.app.drawers;

/**
 * Created by bicou on 19/07/13.
 */
public abstract class MenuItem implements DrawerMenuItemsAdapter.DrawerMenuItem {
	protected DrawerMenuFragment drawerMenuFragment;
	private Object mData;

	public MenuItem(final DrawerMenuFragment drawerMenuFragment) {
		this.drawerMenuFragment = drawerMenuFragment;
	}

	@Override
	public Object getTag() {
		return mData;
	}

	@Override
	public MenuItem setTag(final Object data) {
		this.mData = data;
		return this;
	}

	@Override
	public int getViewType() {
		return DrawerMenuFragment.MENU_VIEWTYPE_ITEM;
	}

	public long getId() {
		return 0;
	}
}
