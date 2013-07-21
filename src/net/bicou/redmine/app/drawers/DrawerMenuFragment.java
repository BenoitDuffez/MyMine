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
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.issues.IssuesListFilter;
import net.bicou.redmine.app.projects.ProjectFragment;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.wiki.WikiActivity;
import net.bicou.redmine.app.wiki.WikiPageFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.*;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 14/06/13.
 */
public class DrawerMenuFragment extends SherlockListFragment {
	DrawerMenuItemsAdapter mAdapter;
	public static final int MENU_VIEWTYPE_SEPARATOR = 0;
	public static final int MENU_VIEWTYPE_PROJECT = 1;
	public static final int MENU_VIEWTYPE_QUERY = 2;
	public static final int MENU_VIEWTYPE_WIKI = 3;
	public static final int MENU_VIEWTYPE_COUNT = 4;

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
		L.d("thread=" + Thread.currentThread());
		buildMenuContents();
		mAdapter.notifyDataSetChanged();
	}

	private void buildMenuContents() {
		mData.clear();
		ProjectsDbAdapter db = new ProjectsDbAdapter(getActivity());
		db.open();
		buildProjects(db);
		buildIssues(db);
		buildWikiPages(db);
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
				mData.add(new MenuItemQuery(DrawerMenuFragment.this, query.name, server.serverUrl).setTag(query));
			}
		}
	}

	private void buildWikiPages(DbAdapter db) {
		mData.add(new MenuSeparator(DrawerMenuFragment.this, R.string.menu_wiki));

		// All pages shortcut
		WikiPage dummy = new WikiPage();
		mData.add(new MenuItemWikiAllPages(DrawerMenuFragment.this).setTag(dummy));

		// List of favorites
		WikiDbAdapter wdb = new WikiDbAdapter(db);
		List<WikiPage> pages = wdb.selectFavorites();
		for (WikiPage page : pages) {
			mData.add(new MenuItemWikiPage(DrawerMenuFragment.this, page.title, page.project.server.serverUrl, page.project.name).setTag(page));
		}
	}

	@Override
	public void onListItemClick(final ListView listView, final View v, final int position, final long id) {
		Object item = mAdapter.getItem(position).getTag();

		if (item instanceof Project) {
			Project project = (Project) item;
			Intent intent = new Intent(getActivity(), ProjectsActivity.class);
			intent.putExtra(ProjectFragment.KEY_PROJECT_JSON, new Gson().toJson(project));
			startActivity(intent);
		} else if (item instanceof Query) {
			Query query = (Query) item;
			Intent intent = new Intent(getActivity(), IssuesActivity.class);
			IssuesListFilter filter = new IssuesListFilter(query.server.rowId, IssuesListFilter.FilterType.QUERY, query.id);
			Bundle args = new Bundle();
			filter.saveTo(args);
			intent.putExtras(args);
			startActivity(intent);
		} else if (item instanceof WikiPage) {
			WikiPage page = (WikiPage) item;
			Intent intent = new Intent(getActivity(), WikiActivity.class);
			if (page.server != null) {
				intent.putExtra(WikiPageFragment.KEY_WIKI_PAGE, new Gson().toJson(page));
			}
			startActivity(intent);
		} else {
			throw new IllegalStateException("Unhandled drawer menu type: " + item);
		}

		DrawerActivity act = (DrawerActivity) getActivity();
		act.closeDrawer();
	}
}
