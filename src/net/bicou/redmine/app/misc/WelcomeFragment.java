package net.bicou.redmine.app.misc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.roadmap.RoadmapActivity;
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

	private static class OverviewCard {
		int titleTextId;
		int subtitleTextId;
		int imageResId;
		int actionIconId;
		int actionTextId;
		Intent actionIntent;
		boolean useSecondAction;
		int action2IconId;
		int action2TextId;
		Intent action2Intent;

		OnClickListener mOnClickListener;

		public OverviewCard setTitle(final int title, final int subtitle, final int image) {
			titleTextId = title;
			subtitleTextId = subtitle;
			imageResId = image;
			return this;
		}

		public OverviewCard addAction(final int actionIcon, final int actionText, final Intent intent) {
			if (actionIconId == 0) {
				actionIconId = actionIcon;
				actionTextId = actionText;
				actionIntent = intent;
			} else {
				useSecondAction = true;
				action2IconId = actionIcon;
				action2TextId = actionText;
				action2Intent = intent;
			}
			return this;
		}

		public View getView(final Context context, final ViewGroup container) {
			TextView title, subTitle, action;
			ImageView image, actionIcon;

			final View card = LayoutInflater.from(context).inflate(R.layout.welcome_card, container, false);

			title = (TextView) card.findViewById(R.id.overview_card_title);
			title.setText(titleTextId);

			subTitle = (TextView) card.findViewById(R.id.overview_card_subtitle);
			subTitle.setId(subtitleTextId);

			image = (ImageView) card.findViewById(R.id.overview_card_image);
			image.setImageResource(imageResId);

			// First action icon
			mOnClickListener = new OnClickListener() {
				@Override
				public void onClick(final View v) {
					try {
						context.startActivity(actionIntent);
					} catch (final Exception e) {
						L.e("Unable to start activity " + actionIntent, e);
					}
				}
			};
			action = (TextView) card.findViewById(R.id.overview_card_action);
			action.setText(actionTextId);
			action.setOnClickListener(mOnClickListener);
			card.setOnClickListener(mOnClickListener);
			card.findViewById(R.id.overview_card_image_holder).setOnClickListener(mOnClickListener);
			card.findViewById(R.id.overview_card_image).setOnClickListener(mOnClickListener);

			actionIcon = (ImageView) card.findViewById(R.id.overview_card_action_image);
			actionIcon.setImageResource(actionIconId);

			// Second action icon
			if (useSecondAction) {
				card.findViewById(R.id.overview_card_second_action_layout).setVisibility(View.VISIBLE);
				action = (TextView) card.findViewById(R.id.overview_card_second_action);
				action.setText(action2TextId);
				action.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						try {
							context.startActivity(action2Intent);
						} catch (final Exception e) {
							L.e("Unable to start activity " + action2Intent);
							// TODO: this message is relevant only for the add account, which is for now, the only second action on a card
							Toast.makeText(context, R.string.setup_sync_help, Toast.LENGTH_LONG).show();
						}
					}
				});

				actionIcon = (ImageView) card.findViewById(R.id.overview_card_second_action_image);
				actionIcon.setImageResource(action2IconId);
			}

			return card;
		}

		public OnClickListener getOnClickListener() {
			return mOnClickListener;
		}
	}

	private static class CardsAdapter extends BaseAdapter {
		private List<OverviewCard> mCards;

		public CardsAdapter(List<OverviewCard> cards) {
			mCards = cards;
		}

		@Override
		public int getCount() {
			return mCards == null ? 0 : mCards.size();
		}

		@Override
		public OverviewCard getItem(int i) {
			return i >= getCount() || i < 0 ? null : mCards.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			OverviewCard item = getItem(i);
			if (item != null) {
				return item.getView(viewGroup.getContext(), viewGroup);
			}
			return null;
		}
	}

	List<OverviewCard> mCards = new ArrayList<OverviewCard>();

	public static WelcomeFragment newInstance(final Bundle args) {
		final WelcomeFragment f = new WelcomeFragment();
		f.setArguments(args);
		return f;
	}

	ViewGroup nowLayout;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_overview, container, false);
		L.d("");
		nowLayout = (ViewGroup) v.findViewById(R.id.overview_nowlayout);

		Intent intent;

		// Issues
		intent = new Intent(getActivity(), IssuesActivity.class);
		mCards.add(new OverviewCard() //
				.setTitle(R.string.overview_card_issues_title, RESID_ISSUES, R.drawable.card_issues) //
				.addAction(R.drawable.icon_issues, R.string.overview_card_issues_action, intent));

		// Projects
		intent = new Intent(getActivity(), ProjectsActivity.class);
		mCards.add(new OverviewCard() //
				.setTitle(R.string.overview_card_projects_title, RESID_PROJECTS, R.drawable.card_project) //
				.addAction(R.drawable.icon_projects, R.string.overview_card_projects_action, intent) //
				.addAction(R.drawable.icon_roadmaps, R.string.overview_card_projects_action2, new Intent(getActivity(), RoadmapActivity.class)));

		// Servers
		intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		intent.putExtra(Settings.EXTRA_AUTHORITIES, SyncUtils.SYNC_AUTHORITIES);
		mCards.add(new OverviewCard() //
				.setTitle(R.string.overview_card_servers_title, RESID_SERVERS, R.drawable.card_server) //
				.addAction(R.drawable.icon_servers, R.string.overview_card_servers_action, intent) //
				.addAction(R.drawable.icon_add, R.string.overview_card_servers_action2, HelpSetupFragment.getNewAccountActivityIntent()));

		// Add the cards views
		//		for (final OverviewCard card : mCards) {
		//			nowLayout.addView(card.getView(getActivity(), container));
		//			nowLayout.addView(getSpacer());
		//		}
		final CardsAdapter adapter = new CardsAdapter(mCards);
		GridView gv = (GridView) v.findViewById(R.id.overview_nowlayout);
		gv.setAdapter(adapter);
		gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				OverviewCard card = adapter.getItem(i);
				if (card != null) {
					card.getOnClickListener().onClick(view);
				}
			}
		});

		refreshUI();

		return v;
	}

	private View getSpacer() {
		final View spacer = new View(getActivity());
		final int size = getResources().getDimensionPixelSize(R.dimen.overview_card_spacer_size);
		spacer.setLayoutParams(new LayoutParams(size, size));
		return spacer;
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
//		((TextView) nowLayout.findViewById(RESID_ISSUES)).setText(String.format(MessageFormat.format(issuesSubTitle, numIssues), getString(whereId)));

		// Load projects
		final ProjectsDbAdapter pdb = new ProjectsDbAdapter(getActivity());
		pdb.open();
		final int numProjects = pdb.getNumProjects();
		pdb.close();
		final String projectsSubTitle = MessageFormat.format(res.getString(R.string.overview_card_projects_subtitle), numProjects);
//		((TextView) nowLayout.findViewById(RESID_PROJECTS)).setText(projectsSubTitle);

		// Load servers
		final int numServers = sdb.getNumServers();
		sdb.close();
		final String serversSubTitle = res.getString(R.string.overview_card_servers_subtitle);
//		((TextView) nowLayout.findViewById(RESID_SERVERS)).setText(MessageFormat.format(serversSubTitle, numServers));
	}
}
