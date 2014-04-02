package net.bicou.redmine.app.welcome;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.origamilabs.library.views.StaggeredGridView;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.issues.edit.ServerProjectPickerDialog;
import net.bicou.redmine.app.issues.edit.ServerProjectPickerFragment;
import net.bicou.redmine.app.misc.HelpSetupFragment;
import net.bicou.redmine.app.misc.MainActivity;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.roadmap.RoadmapActivity;
import net.bicou.redmine.app.wiki.WikiActivity;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.sync.SyncUtils;
import net.bicou.redmine.util.L;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;

public class WelcomeFragment extends TrackedFragment {
	public static WelcomeFragment newInstance(final Bundle args) {
		final WelcomeFragment f = new WelcomeFragment();
		f.setArguments(args);
		return f;
	}

	StaggeredGridView mStaggeredGridView;
	CardsAdapter mAdapter;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_overview, container, false);
		L.d("");
		mStaggeredGridView = (StaggeredGridView) v.findViewById(R.id.overview_container);

		// Add the cards views
		mAdapter = new CardsAdapter().setCallbacks(mCardsActionsCallback);
		mStaggeredGridView.setAdapter(mAdapter);
		mStaggeredGridView.setOnItemClickListener(new StaggeredGridView.OnItemClickListener() {
			@Override
			public void onItemClick(StaggeredGridView parent, View view, int position, long id) {
				L.d("view=" + view + " i=" + position);
				OverviewCard card = mAdapter.getItem(position);
				if (card != null) {
					startActivity(card.getDefaultAction());
				}
			}
		});
		mStaggeredGridView.setClickable(true);
		mStaggeredGridView.setDrawSelectorOnTop(true);
		mStaggeredGridView.setLongClickable(true);
		mStaggeredGridView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				L.d("view=" + view);
			}
		});

		AsyncTaskFragment.runTask((ActionBarActivity) getActivity(), MainActivity.ACTION_REFRESH_MAIN_SCREEN, null);
		return v;
	}

	public enum WelcomeCardAction {
		ROADMAPS,
		ADD_SERVER,
		ADD_ISSUE,
		SHOW_WIKI,
	}

	CardsAdapter.CardActionCallback mCardsActionsCallback = new CardsAdapter.CardActionCallback() {
		@Override
		public void onActionSelected(Object action) {
			switch ((WelcomeCardAction) action) {
			case ROADMAPS:
				startActivity(new Intent(getActivity(), RoadmapActivity.class));
				break;

			case ADD_SERVER:
				startActivity(HelpSetupFragment.getNewAccountActivityIntent());
				break;

			case ADD_ISSUE:
				DialogFragment newFragment = ServerProjectPickerFragment.newInstance(ServerProjectPickerDialog.DesiredSelection.SERVER_PROJECT);
				newFragment.show(getActivity().getSupportFragmentManager(), "serverProjectPicker");
				break;

			case SHOW_WIKI:
				startActivity(new Intent(getActivity(), WikiActivity.class));
				break;
			}

			// Prevent crouton from showing if we display another view (likely to hide the "modifications discarded" crouton back from
			// EditIssueActivity)
			Crouton.cancelAllCroutons();
		}
	};

	/**
	 * Called from a background thread
	 */
	public static List<OverviewCard> buildCards(Context ctx) {
		String issuesDescription, projectsDescription, serversDescription;
		List<OverviewCard> cards = new ArrayList<OverviewCard>();

		final Resources res = ctx.getResources();
		final ServersDbAdapter sdb = new ServersDbAdapter(ctx);
		sdb.open();

		// Load issues
		final List<Server> servers = sdb.selectAll();
		int numIssuesTotal = 0, numIssuesMyself = 0;
		final IssuesDbAdapter idb = new IssuesDbAdapter(sdb);
		numIssuesTotal = idb.countAll();

		for (final Server server : servers) {
			if (server.user != null) {
				numIssuesMyself += idb.countIssues(server, server.user);
			}
		}

		// Build description strings
		String issuesSubTitle = res.getString(R.string.overview_card_issues_subtitle).replace("'", "‘");
		issuesDescription = String.format(MessageFormat.format(issuesSubTitle, numIssuesTotal), ctx.getString(R.string.overview_card_issues_count_all));
		if (numIssuesMyself > 0) {
			issuesDescription = String.format(MessageFormat.format(issuesSubTitle, numIssuesMyself), ctx.getString(R.string.overview_card_issues_count_all));
		}

		// Load projects
		final ProjectsDbAdapter pdb = new ProjectsDbAdapter(sdb);
		final int numProjects = pdb.getNumProjects();
		String projectsSubTitle = res.getString(R.string.overview_card_projects_subtitle).replace("'", "‘");
		projectsDescription = MessageFormat.format(projectsSubTitle, numProjects);

		// Load servers
		final int numServers = sdb.getNumServers();
		sdb.close();
		final String serversSubTitle = res.getString(R.string.overview_card_servers_subtitle).replace("'", "‘");
		serversDescription = MessageFormat.format(serversSubTitle, numServers);

		// Issues
		cards.add(new OverviewCard(new Intent(ctx, IssuesActivity.class)) //
				.setContent(R.string.overview_card_issues_title, issuesDescription, R.drawable.card_issues, R.drawable.icon_issues) //
				.addAction(WelcomeCardAction.ADD_ISSUE, R.string.menu_issues_add));

		// Projects
		cards.add(new OverviewCard(new Intent(ctx, ProjectsActivity.class)) //
				.setContent(R.string.overview_card_projects_title, projectsDescription, R.drawable.card_project, R.drawable.icon_projects) //
						//				.addAction(ID_PROJECTS, R.string.overview_card_projects_action) //
				.addAction(WelcomeCardAction.ROADMAPS, R.string.overview_card_projects_see_roadmaps) //
				.addAction(WelcomeCardAction.SHOW_WIKI, R.string.overview_card_projects_see_wiki));

		// Servers
		Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		intent.putExtra(Settings.EXTRA_AUTHORITIES, SyncUtils.SYNC_AUTHORITIES);
		cards.add(new OverviewCard(intent) //
				.setContent(R.string.overview_card_servers_title, serversDescription, R.drawable.card_server, R.drawable.icon_servers) //
						//				.addAction(ID_SYNC, R.string.overview_card_servers_sync) //4
				.addAction(WelcomeCardAction.ADD_SERVER, R.string.overview_card_servers_add));

		return cards;
	}

	public void onCardsBuilt(List<OverviewCard> cards) {
		mAdapter.setData(cards);
	}
}
