package net.bicou.redmine.app.drawers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.misc.AboutActivity;
import net.bicou.redmine.app.misc.MainActivity;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.roadmap.RoadmapActivity;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.app.wiki.WikiActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 14/06/13.
 */
public class DrawerMenuFragment extends SherlockListFragment {
	DrawerMenuItemsAdapter mAdapter;

	private List<DrawerMenuItemsAdapter.DrawerMenuItem> mData = new ArrayList<DrawerMenuItemsAdapter.DrawerMenuItem>() {{
		add(new MenuSeparator(DrawerMenuFragment.this, R.string.app_name));
		add(new MenuItem(DrawerMenuFragment.this, R.drawable.icon_projects, R.string.menu_projects));
		add(new MenuItem(DrawerMenuFragment.this, R.drawable.icon_issues, R.string.menu_issues));
		add(new MenuItem(DrawerMenuFragment.this, R.drawable.icon_roadmaps, R.string.menu_roadmap));
		add(new MenuItem(DrawerMenuFragment.this, R.drawable.icon_wiki, R.string.menu_wiki));
	}};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_drawer, container, false);
		mAdapter = new DrawerMenuItemsAdapter(getActivity(), mData);
		setListAdapter(mAdapter);
		return v;
	}

	@Override
	public void onListItemClick(final ListView listView, final View v, final int position, final long id) {
		final Bundle args = new Bundle();
		final Intent intent;

		switch (mAdapter.getItem(position).getTextId()) {
		case R.string.menu_issues:
			intent = new Intent(listView.getContext(), IssuesActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_projects:
			intent = new Intent(listView.getContext(), ProjectsActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_roadmap:
			intent = new Intent(listView.getContext(), RoadmapActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_wiki:
			intent = new Intent(listView.getContext(), WikiActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_about:
			intent = new Intent(listView.getContext(), AboutActivity.class);
			break;

		case R.string.menu_settings:
			intent = new Intent(listView.getContext(), SettingsActivity.class);
			break;

		default:
			intent = new Intent(listView.getContext(), MainActivity.class);
			break;
		}

		startActivity(intent);
		DrawerActivity act = (DrawerActivity) getActivity();
		act.closeDrawer();
	}
}
