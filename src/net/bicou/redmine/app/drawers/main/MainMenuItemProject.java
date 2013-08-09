package net.bicou.redmine.app.drawers.main;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
public class MainMenuItemProject extends MainMenuItem<DrawerMenuFragment.DrawerMenuViewType> {
	String mText;

	private class ViewHolder {
		ImageView icon;
		TextView text;
	}

	public MainMenuItemProject(DrawerMenuFragment drawerMenuFragment, String text) {
		super(drawerMenuFragment, DrawerMenuFragment.DrawerMenuViewType.PROJECT);
		mText = text;
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
