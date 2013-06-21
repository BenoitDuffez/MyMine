package net.bicou.redmine.app.misc;

import android.content.Intent;
import android.content.res.Resources;
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
	private static final int RESID_ISSUES = 0x1B00B;
	private static final int RESID_PROJECTS = 0x2B00B5;
	private static final int RESID_SERVERS = 0x3B00B5;


	public static WelcomeFragment newInstance(final Bundle args) {
		final WelcomeFragment f = new WelcomeFragment();
		f.setArguments(args);
		return f;
	}

	StaggeredGridView mStaggeredGridView;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_overview, container, false);
		L.d("");
		mStaggeredGridView = (StaggeredGridView) v.findViewById(R.id.overview_container);

		Intent intent;
		List<OverviewCard> cards = new ArrayList<OverviewCard>();

		// Issues
		intent = new Intent(getActivity(), IssuesActivity.class);
		cards.add(new OverviewCard() //
				.setTitle(R.string.overview_card_issues_title, RESID_ISSUES, R.drawable.card_issues) //
				.addAction(R.drawable.icon_issues, R.string.overview_card_issues_action, intent));

		// Projects
		intent = new Intent(getActivity(), ProjectsActivity.class);
		cards.add(new OverviewCard() //
				.setTitle(R.string.overview_card_projects_title, RESID_PROJECTS, R.drawable.card_project) //
				.addAction(R.drawable.icon_projects, R.string.overview_card_projects_action, intent) //
				.addAction(R.drawable.icon_roadmaps, R.string.overview_card_projects_action2, new Intent(getActivity(), RoadmapActivity.class)));

		// Servers
		intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		intent.putExtra(Settings.EXTRA_AUTHORITIES, SyncUtils.SYNC_AUTHORITIES);
		cards.add(new OverviewCard() //
				.setTitle(R.string.overview_card_servers_title, RESID_SERVERS, R.drawable.card_server) //
				.addAction(R.drawable.icon_servers, R.string.overview_card_servers_action, intent) //
				.addAction(R.drawable.icon_add, R.string.overview_card_servers_action2, HelpSetupFragment.getNewAccountActivityIntent()));

		// Add the cards views
		final CardsAdapter adapter = new CardsAdapter(cards);
		mStaggeredGridView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		mStaggeredGridView.setOnItemClickListener(new StaggeredGridView.OnItemClickListener() {
			@Override
			public void onItemClick(StaggeredGridView parent, View view, int position, long id) {
				OverviewCard card = adapter.getItem(position);
				if (card != null) {
					card.getOnClickListener().onClick(view);
				}
			}
		});

		refreshUI();
		v.invalidate();

		return v;
	}

	public void refreshUI() {
		final Resources res = getResources();
		final ServersDbAdapter sdb = new ServersDbAdapter(getActivity());
		sdb.open();

		// Load issues
		final List<Server> servers = sdb.selectAll();
		int numIssues = 0;
		boolean isAnon = true;
		final IssuesDbAdapter idb = new IssuesDbAdapter(getActivity());
		idb.open();
		for (final Server server : servers) {
			if (server.user != null) {
				isAnon = false;
				numIssues += idb.countIssues(server, server.user);
			}
		}
		if (isAnon) {
			numIssues = idb.countAll();
		}
		final String issuesSubTitle = res.getString(R.string.overview_card_issues_subtitle);
		final int whereId = isAnon ? R.string.overview_card_issues_anonymous : R.string.overview_card_issues_logged;
		//		((TextView) mStaggeredGridView.findViewById(RESID_ISSUES)).setText(String.format(MessageFormat.format(issuesSubTitle, numIssues),
		// getString(whereId)));

		// Load projects
		final ProjectsDbAdapter pdb = new ProjectsDbAdapter(getActivity());
		pdb.open();
		final int numProjects = pdb.getNumProjects();
		pdb.close();
		final String projectsSubTitle = MessageFormat.format(res.getString(R.string.overview_card_projects_subtitle), numProjects);
		//		((TextView) mStaggeredGridView.findViewById(RESID_PROJECTS)).setText(projectsSubTitle);

		// Load servers
		final int numServers = sdb.getNumServers();
		sdb.close();
		final String serversSubTitle = res.getString(R.string.overview_card_servers_subtitle);
		//		((TextView) mStaggeredGridView.findViewById(RESID_SERVERS)).setText(MessageFormat.format(serversSubTitle, numServers));
	}
}
