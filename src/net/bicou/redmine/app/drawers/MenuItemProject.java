package net.bicou.redmine.app.drawers;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
public class MenuItemProject extends MenuItem {
	String mText;

	private class ViewHolder {
		ImageView icon;
		TextView text;
	}

	public MenuItemProject(DrawerMenuFragment drawerMenuFragment, String text) {
		super(drawerMenuFragment);
		mText = text;
	}

	@Override
	public int getViewType() {
		return DrawerMenuFragment.MENU_VIEWTYPE_PROJECT;
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
		holder.text.setText(mText);

		return v;
	}
}
