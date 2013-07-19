package net.bicou.redmine.app.drawers;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
class MenuItem implements DrawerMenuItemsAdapter.DrawerMenuItem {
	private DrawerMenuFragment drawerMenuFragment;
	int iconId, textId;

	private class ViewHolder {
		ImageView icon;
		TextView text;
	}

	public MenuItem(final DrawerMenuFragment drawerMenuFragment, int icon, int text) {
		this.drawerMenuFragment = drawerMenuFragment;
		iconId = icon;
		textId = text;
	}

	@Override
	public View fillView(View convertView, ViewGroup parent) {
		View v;
		ViewHolder holder;

		if (convertView == null) {
			v = drawerMenuFragment.getActivity().getLayoutInflater().inflate(R.layout.drawer_menu_item, parent, false);
			holder = new ViewHolder();
			holder.icon = (ImageView) v.findViewById(R.id.slidingmenu_item_icon);
			holder.text = (TextView) v.findViewById(R.id.slidingmenu_item_text);
			v.setTag(holder);
		} else {
			v = convertView;
			holder = (ViewHolder) convertView.getTag();
		}

		holder.icon.setImageResource(iconId);
		holder.text.setText(textId);

		return v;
	}

	@Override
	public long getId() {
		return textId;
	}

	@Override
	public int getViewType() {
		return DrawerMenuFragment.MENU_VIEWTYPE_ITEM;
	}

	@Override
	public int getTextId() {
		return textId;
	}
}
