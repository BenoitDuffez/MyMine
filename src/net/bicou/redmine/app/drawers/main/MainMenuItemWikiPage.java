package net.bicou.redmine.app.drawers.main;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
public class MainMenuItemWikiPage extends MainMenuItem<DrawerMenuFragment.DrawerMenuViewType> {
	String server, page, project;

	public MainMenuItemWikiPage(final DrawerMenuFragment drawerMenuFragment, String pageName, String serverName, String projectName) {
		super(drawerMenuFragment, DrawerMenuFragment.DrawerMenuViewType.WIKI);
		page = pageName;
		server = serverName;
		project = projectName;
	}

	private static class ViewHolder {
		TextView server, page, project;
	}

	@Override
	public View fillView(View convertView, ViewGroup parent) {
		View v;
		ViewHolder holder;

		if (convertView == null) {
			v = drawerMenuFragment.getActivity().getLayoutInflater().inflate(R.layout.drawer_menu_wiki_page, parent, false);
			holder = new ViewHolder();
			holder.page = (TextView) v.findViewById(R.id.drawer_wiki_name);
			holder.server = (TextView) v.findViewById(R.id.drawer_wiki_server);
			holder.project = (TextView) v.findViewById(R.id.drawer_wiki_project);
			v.setTag(holder);
		} else {
			v = convertView;
			holder = (ViewHolder) convertView.getTag();
		}

		holder.page.setText(page);
		holder.server.setText(server.replace("http://", "").replace("https://", ""));
		holder.project.setText(project);

		return v;
	}
}
