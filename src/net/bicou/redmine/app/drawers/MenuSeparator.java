package net.bicou.redmine.app.drawers;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
class MenuSeparator implements DrawerMenuItemsAdapter.DrawerMenuItem {
	private DrawerMenuFragment drawerMenuFragment;
	int textId;

	public MenuSeparator(final DrawerMenuFragment drawerMenuFragment, int text) {
		this.drawerMenuFragment = drawerMenuFragment;
		textId = text;
	}

	@Override
	public View fillView(View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = drawerMenuFragment.getActivity().getLayoutInflater().inflate(R.layout.drawer_menu_separator, parent, false);
		} else {
			v = convertView;
		}

		TextView tv = (TextView) v.findViewById(R.id.slidingmenu_item_text);
		tv.setText(textId);

		return v;
	}

	@Override
	public long getId() {
		return textId;
	}

	@Override
	public int getViewType() {
		return DrawerMenuFragment.MENU_VIEWTYPE_SEPARATOR;
	}

	@Override
	public DrawerMenuItemsAdapter.DrawerMenuItem setTag(final Object tag) {
		return null;
	}

	@Override
	public Object getTag() {
		return null;
	}
}
