package net.bicou.redmine.app.drawers.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.google.gson.Gson;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.drawers.DrawerActivity;
import net.bicou.redmine.app.drawers.DrawerMenuItem;
import net.bicou.redmine.app.drawers.DrawerMenuItemsAdapter;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.issues.IssuesListFilter;
import net.bicou.redmine.app.projects.ProjectFragment;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.wiki.WikiActivity;
import net.bicou.redmine.app.wiki.WikiPageFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.QueriesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 14/06/13.
 */
public class DrawerMenuFragment extends TrackedListFragment {
	DrawerMenuItemsAdapter<DrawerMenuViewType> mAdapter;

	public enum DrawerMenuViewType {
		SEPARATOR,
		PROJECT,
		QUERY,
		WIKI,
	}

	private List<DrawerMenuItem<DrawerMenuViewType>> mData = new ArrayList<DrawerMenuItem<DrawerMenuViewType>>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_drawer, container, false);

		buildMenuContents();

		mAdapter = new DrawerMenuItemsAdapter<DrawerMenuViewType>(getActivity(), mData);
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
		mData.add(new MainMenuSeparator(DrawerMenuFragment.this, R.string.menu_projects));
		ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);
		List<Project> favs = pdb.getFavorites();

		if (favs.size() <= 0) {
			mData.add(new MainMenuItemProject(this, getString(R.string.drawer_main_no_favorite)));
		} else {
			for (Project project : favs) {
				mData.add(new MainMenuItemProject(this, project.name).setTag(project));
			}
		}
	}

	private void buildIssues(DbAdapter db) {
		mData.add(new MainMenuSeparator(DrawerMenuFragment.this, R.string.menu_issues));
		QueriesDbAdapter qdb = new QueriesDbAdapter(db);
		ServersDbAdapter sdb = new ServersDbAdapter(db);
		IssuesDbAdapter idb = new IssuesDbAdapter(db);

		List<Server> servers = sdb.selectAll();
		List<Issue> issues = new ArrayList<Issue>();
		for (Server server : servers) {
			List<Query> queries = qdb.selectAll(server);
			for (Query query : queries) {
				mData.add(new MainMenuItemQuery(this, query.name, server.serverUrl).setTag(query));
			}
			issues.addAll(idb.getFavorites(server));
		}

		if (issues.size() <= 0) {
			mData.add(new MainMenuItemQuery(this, getString(R.string.drawer_main_no_favorite), ""));
		} else {
			for (Issue issue : issues) {
				mData.add(new MainMenuItemQuery(this, issue.subject, issue.project.name).setTag(issue));
			}
		}
	}

	private void buildWikiPages(DbAdapter db) {
		mData.add(new MainMenuSeparator(DrawerMenuFragment.this, R.string.menu_wiki));

		// All pages shortcut
		WikiPage dummy = new WikiPage();
		MainMenuItem<DrawerMenuViewType> allWikiPages = new MainMenuItemWikiPage(this, getString(R.string.drawer_wiki_all_pages), null, null).setTag(dummy);

		// List of favorites
		WikiDbAdapter wdb = new WikiDbAdapter(db);
		List<WikiPage> pages = wdb.selectFavorites();
		if (pages.size() <= 0) {
			mData.add(new MainMenuItemWikiPage(this, getString(R.string.drawer_main_no_favorite), "", ""));
			mData.add(allWikiPages);
		} else {
			mData.add(allWikiPages);
			for (WikiPage page : pages) {
				mData.add(new MainMenuItemWikiPage(this, page.title, page.project.server.serverUrl, page.project.name).setTag(page));
			}
		}
	}

	@Override
	public void onListItemClick(final ListView listView, final View v, final int position, final long id) {
		Object item = mAdapter.getItem(position).getTag();

		if (item == null) {
			// No-op
		} else if (item instanceof Project) {
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
				intent.putExtra(Constants.KEY_SERVER_ID, page.server.rowId);
				intent.putExtra(Constants.KEY_PROJECT_ID, page.project.id);
				intent.putExtra(WikiPageFragment.KEY_WIKI_PAGE, new Gson().toJson(page));
			}
			startActivity(intent);
		} else if (item instanceof Issue) {
			Intent intent = new Intent(getActivity(), IssuesActivity.class);
			intent.putExtra(Constants.KEY_ISSUE_ID, ((Issue) item).id);
			intent.putExtra(Constants.KEY_SERVER_ID, ((Issue) item).server.rowId);
			startActivity(intent);
		} else {
			throw new IllegalStateException("Unhandled drawer menu type: " + item);
		}

		DrawerActivity act = (DrawerActivity) getActivity();
		act.closeDrawer();
	}
}
