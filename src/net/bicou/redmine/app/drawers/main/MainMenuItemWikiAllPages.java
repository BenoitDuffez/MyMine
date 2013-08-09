package net.bicou.redmine.app.drawers.main;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/07/13.
 */
public class MainMenuItemWikiAllPages extends MainMenuItemWikiPage {
	public MainMenuItemWikiAllPages(final DrawerMenuFragment drawerMenuFragment) {
		super(drawerMenuFragment, drawerMenuFragment.getString(R.string.drawer_wiki_all_pages), null, null);
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
		holder.server.setVisibility(View.GONE);
		holder.project.setVisibility(View.GONE);

		return v;
	}
}
