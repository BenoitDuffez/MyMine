package net.bicou.redmine.app.drawers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.google.gson.Gson;
import net.bicou.redmine.R;
import net.bicou.redmine.app.projects.ProjectFragment;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 14/06/13.
 */
public class DrawerMenuFragment extends SherlockListFragment {
	DrawerMenuItemsAdapter mAdapter;
	public static final int MENU_VIEWTYPE_SEPARATOR = 0;
	public static final int MENU_VIEWTYPE_ITEM = 1;

	private List<DrawerMenuItemsAdapter.DrawerMenuItem> mData = new ArrayList<DrawerMenuItemsAdapter.DrawerMenuItem>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_drawer, container, false);

		buildMenuContents();

		mAdapter = new DrawerMenuItemsAdapter(getActivity(), mData);
		setListAdapter(mAdapter);
		return v;
	}

	public void refreshMenu() {
		buildMenuContents();
		mAdapter.notifyDataSetChanged();
	}

	private void buildMenuContents() {
		mData.clear();
		ProjectsDbAdapter db = new ProjectsDbAdapter(getActivity());
		db.open();
		buildProjects(db);
		buildIssues(db);
		db.close();
	}

	private void buildProjects(final DbAdapter db) {
		mData.add(new MenuSeparator(DrawerMenuFragment.this, R.string.menu_projects));
		ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);
		List<Project> favs = pdb.getFavorites();

		for (Project project : favs) {
			mData.add(new MenuItemProject(DrawerMenuFragment.this, project.name).setTag(project));
		}
	}

	private void buildIssues(DbAdapter db) {
		mData.add(new MenuSeparator(DrawerMenuFragment.this, R.string.menu_issues));
		QueriesDbAdapter qdb = new QueriesDbAdapter(db);
		ServersDbAdapter sdb = new ServersDbAdapter(db);
		List<Server> servers = sdb.selectAll();
		for (Server server : servers) {
			List<Query> queries = qdb.selectAll(server);
			for (Query query : queries) {
				mData.add(new MenuItemQuery(DrawerMenuFragment.this, query.name, server.serverUrl));
			}
		}
	}

	@Override
	public void onListItemClick(final ListView listView, final View v, final int position, final long id) {
		Project project = (Project) mAdapter.getItem(position).getTag();

		Intent intent = new Intent(getActivity(), ProjectsActivity.class);
		intent.putExtra(ProjectFragment.KEY_PROJECT_JSON, new Gson().toJson(project));
		startActivity(intent);

		DrawerActivity act = (DrawerActivity) getActivity();
		act.closeDrawer();
	}
}
