package net.bicou.redmine.app.drawers;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
public class MenuItemQuery extends MenuItem {
	String server, query;

	public MenuItemQuery(final DrawerMenuFragment drawerMenuFragment, String queryName, String serverName) {
		super(drawerMenuFragment);
		query = queryName;
		server = serverName;
	}

	private static class ViewHolder {
		TextView server, query;
	}

	@Override
	public int getViewType() {
		return DrawerMenuFragment.MENU_VIEWTYPE_QUERY;
	}

	@Override
	public View fillView(View convertView, ViewGroup parent) {
		View v;
		ViewHolder holder;

		if (convertView == null) {
			v = drawerMenuFragment.getActivity().getLayoutInflater().inflate(R.layout.drawer_menu_issue_query, parent, false);
			holder = new ViewHolder();
			holder.query = (TextView) v.findViewById(R.id.drawer_query_name);
			holder.server = (TextView) v.findViewById(R.id.drawer_query_server);
			v.setTag(holder);
		} else {
			v = convertView;
			holder = (ViewHolder) convertView.getTag();
		}

		holder.query.setText(query);
		holder.server.setText(server.replace("http://", "").replace("https://", ""));

		return v;
	}
}
