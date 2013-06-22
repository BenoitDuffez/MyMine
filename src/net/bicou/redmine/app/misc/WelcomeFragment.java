package net.bicou.redmine.app.misc;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.origamilabs.library.views.StaggeredGridView;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.roadmap.RoadmapActivity;
import net.bicou.redmine.app.welcome.CardsAdapter;
import net.bicou.redmine.app.welcome.OverviewCard;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.sync.SyncUtils;
import net.bicou.redmine.util.L;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class WelcomeFragment extends Fragment {
	public static WelcomeFragment newInstance(final Bundle args) {
		final WelcomeFragment f = new WelcomeFragment();
		f.setArguments(args);
		return f;
	}

	StaggeredGridView mStaggeredGridView;
	List<OverviewCard> mCards = new ArrayList<OverviewCard>();

	final int ID_PROJECTS = 10, ID_ROADMAPS = 11;
	final int ID_SERVERS_ADD = 20, ID_SYNC = 21;
	CardsAdapter.CardActionCallback mCardsActionsCallback = new CardsAdapter.CardActionCallback() {
		@Override
		public void onActionSelected(int actionId) {
			switch (actionId) {
			case ID_ROADMAPS:
				startActivity(new Intent(getActivity(), RoadmapActivity.class));
				break;

			case ID_SERVERS_ADD:
				startActivity(HelpSetupFragment.getNewAccountActivityIntent());
				break;
			}
		}
	};

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_overview, container, false);
		L.d("");
		mStaggeredGridView = (StaggeredGridView) v.findViewById(R.id.overview_container);

		// Add the cards views
		final CardsAdapter adapter = new CardsAdapter(mCards, mCardsActionsCallback);
		mStaggeredGridView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		mStaggeredGridView.setOnItemClickListener(new StaggeredGridView.OnItemClickListener() {
			@Override
			public void onItemClick(StaggeredGridView parent, View view, int position, long id) {
				OverviewCard card = adapter.getItem(position);
				if (card != null) {
					startActivity(card.getDefaultAction());
				}
			}
		});


		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				buildCards();
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				adapter.notifyDataSetChanged();
			}
		}.execute();

		return v;
	}

	private void buildCards() {
		String issuesDescription, projectsDescription, serversDescription;

		final Resources res = getResources();
		final ServersDbAdapter sdb = new ServersDbAdapter(getActivity());
		sdb.open();

		// Load issues
		final List<Server> servers = sdb.selectAll();
		int numIssues = 0;
		boolean isAnon = true;
		final IssuesDbAdapter idb = new IssuesDbAdapter(sdb);
		if (isAnon) {
			numIssues = idb.countAll();
		} else {
			for (final Server server : servers) {
				if (server.user != null) {
					isAnon = false;
					numIssues += idb.countIssues(server, server.user);
				}
			}
		}

		// Build description strings
		final String issuesSubTitle = res.getString(R.string.overview_card_issues_subtitle);
		final int whereId = isAnon ? R.string.overview_card_issues_anonymous : R.string.overview_card_issues_logged;
		issuesDescription = String.format(MessageFormat.format(issuesSubTitle, numIssues), getString(whereId));

		// Load projects
		final ProjectsDbAdapter pdb = new ProjectsDbAdapter(sdb);
		final int numProjects = pdb.getNumProjects();
		projectsDescription = MessageFormat.format(res.getString(R.string.overview_card_projects_subtitle), numProjects);

		// Load servers
		final int numServers = sdb.getNumServers();
		sdb.close();
		final String serversSubTitle = res.getString(R.string.overview_card_servers_subtitle);
		serversDescription = MessageFormat.format(serversSubTitle, numServers);

		// Issues
		mCards.add(new OverviewCard(new Intent(getActivity(), IssuesActivity.class)) //
				.setContent(R.string.overview_card_issues_title, issuesDescription, R.drawable.card_issues, R.drawable.icon_issues));

		// Projects
		mCards.add(new OverviewCard(new Intent(getActivity(), ProjectsActivity.class)) //
				.setContent(R.string.overview_card_projects_title, projectsDescription, R.drawable.card_project, R.drawable.icon_projects) //
						//				.addAction(ID_PROJECTS, R.string.overview_card_projects_action) //
				.addAction(ID_ROADMAPS, R.string.overview_card_projects_action2));

		// Servers
		Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		intent.putExtra(Settings.EXTRA_AUTHORITIES, SyncUtils.SYNC_AUTHORITIES);
		mCards.add(new OverviewCard(intent) //
				.setContent(R.string.overview_card_servers_title, serversDescription, R.drawable.card_server, R.drawable.icon_servers) //
						//				.addAction(ID_SYNC, R.string.overview_card_servers_sync) //
				.addAction(ID_SERVERS_ADD, R.string.overview_card_servers_add));
	}
}
