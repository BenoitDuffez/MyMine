package net.bicou.redmine.app.roadmap;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AbsMyMineActivity;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter;
import net.bicou.redmine.app.issues.IssuesOrderingFragment;
import net.bicou.redmine.data.json.Version;

import java.util.ArrayList;

public class RoadmapActivity extends AbsMyMineActivity implements RoadmapsListFragment.RoadmapSelectionListener {
	/** Whether the screen is split into a list + an item. Likely the case on tablets and/or in landscape orientation */
	public static final String KEY_IS_SPLIT_SCREEN = "net.bicou.redmine.app.roadmap.SplitScreen";
	boolean mIsSplitScreen;

	@Override
	public void onPreCreate() {
		prepareIndeterminateProgressActionBar();
	}

	@Override
	public void onRoadmapSelected(Version version) {
		int fragId = mIsSplitScreen ? R.id.roadmaps_pane_roadmap : R.id.roadmaps_pane_list;
		Bundle args = new Bundle();
		args.putString(RoadmapFragment.KEY_VERSION_JSON, new Gson().toJson(version));
		getSupportFragmentManager().beginTransaction().replace(fragId, RoadmapFragment.newInstance(args)).addToBackStack("prout").commit();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_roadmaps);

		mIsSplitScreen = findViewById(R.id.roadmaps_pane_roadmap) != null;
		final Bundle args = new Bundle();
		args.putBoolean(KEY_IS_SPLIT_SCREEN, mIsSplitScreen);

		// Setup fragments
		if (savedInstanceState == null) {
			// Setup list view
			getSupportFragmentManager().beginTransaction().replace(R.id.roadmaps_pane_list, RoadmapsListFragment.newInstance(args)).commit();
		}

		// Screen rotation on 7" tablets
		if (savedInstanceState != null && mIsSplitScreen != savedInstanceState.getBoolean(KEY_IS_SPLIT_SCREEN)) {
			final Fragment f = getSupportFragmentManager().findFragmentById(R.id.roadmaps_pane_list);
			if (f != null && f instanceof RoadmapsListFragment) {
				// TODO ((RoadmapsListFragment) f).updateSplitScreenState(mIsSplitScreen);
			}
		}
	}

	@Override
	protected void onCurrentProjectChanged() {
		FragmentManager fm = getSupportFragmentManager();
		Fragment f = fm.findFragmentById(R.id.roadmaps_pane_list);
		if (f != null && f instanceof RoadmapsListFragment) {
			((RoadmapsListFragment) f).updateRoadmapsList();
		} else {
			fm.beginTransaction().replace(R.id.roadmaps_pane_list, RoadmapsListFragment.newInstance(new Bundle())).commit();
		}
	}

	@Override
	protected boolean shouldDisplayProjectsSpinner() {
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = mIsSplitScreen ? R.id.issues_pane_issue : R.id.issues_pane_list;
		final Fragment frag = getSupportFragmentManager().findFragmentById(id);

		switch (item.getItemId()) {
		case R.id.menu_roadmap_sort_issues:
			if (frag instanceof RoadmapFragment) {
				final RoadmapFragment rf = (RoadmapFragment) frag;
				final IssuesOrderingFragment issuesOrder = IssuesOrderingFragment.newInstance(rf.getCurrentOrder());
				issuesOrder.setOrderSelectionListener(new IssuesOrderingFragment.IssuesOrderSelectionListener() {
					@Override
					public void onOrderColumnsSelected(final ArrayList<IssuesOrderColumnsAdapter.OrderColumn> orderColumns) {
						rf.setNewIssuesOrder(orderColumns);

						final FragmentManager fm = getSupportFragmentManager();
						final Fragment frag = fm.findFragmentById(R.id.issues_pane_list);
					}
				});
				issuesOrder.show(getSupportFragmentManager(), "issues_order");
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		final Fragment frag = getSupportFragmentManager().findFragmentById(R.id.roadmaps_pane_list);
		final Bundle args = new Bundle();
		args.putBoolean(KEY_IS_SPLIT_SCREEN, mIsSplitScreen);
		if (frag instanceof RoadmapsListFragment) {
			// ((RoadmapsListFragment) frag).updateCurrentRoadmap(mRoadmaps.get(mCurrentRoadmapPosition).id);
			if (mIsSplitScreen) {
				final Fragment f = getSupportFragmentManager().findFragmentById(R.id.roadmaps_pane_roadmap);
				if (f != null) {
					getSupportFragmentManager().beginTransaction().remove(f).commit();
				}
			}
		} else if (frag instanceof RoadmapFragment) {
			getSupportFragmentManager().beginTransaction().replace(R.id.roadmaps_pane_list, RoadmapsListFragment.newInstance(args)).commit();
		}
		supportInvalidateOptionsMenu();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IS_SPLIT_SCREEN, mIsSplitScreen);
	}
}
