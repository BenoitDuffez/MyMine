package net.bicou.redmine.app.projects;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import net.bicou.redmine.R;
import net.bicou.redmine.app.drawers.DrawerActivity;

public class ProjectsActivity extends DrawerActivity {
	/** Whether the screen is split into a list + an item. Likely the case on tablets and/or in landscape orientation */
	public static final String KEY_IS_SPLIT_SCREEN = "net.bicou.redmine.projects.SplitScreen";
	boolean mIsSplitScreen;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_drawer);//TODO:split

		mIsSplitScreen = findViewById(R.id.projects_pane_project) != null;
		final Bundle args = new Bundle();
		args.putBoolean(KEY_IS_SPLIT_SCREEN, mIsSplitScreen);

		// Setup fragments
		if (savedInstanceState == null) {
			// Setup list view
			getSupportFragmentManager().beginTransaction().replace(R.id.projects_pane_list, ProjectsListFragment.newInstance(args)).commit();
		} else if (savedInstanceState.containsKey(ProjectFragment.KEY_PROJECT_JSON)) {
			// Setup content view, if possible
			if (mIsSplitScreen) {
				getSupportFragmentManager().beginTransaction().replace(R.id.projects_pane_project, ProjectFragment.newInstance(args)).commit();
			}
		}

		// Screen rotation on 7" tablets
		if (savedInstanceState != null && mIsSplitScreen != savedInstanceState.getBoolean(KEY_IS_SPLIT_SCREEN)) {
			final Fragment f = getSupportFragmentManager().findFragmentById(R.id.projects_pane_list);
			if (f != null && f instanceof ProjectsListFragment) {
				((ProjectsListFragment) f).updateSplitScreenState(mIsSplitScreen);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		final Fragment frag = getSupportFragmentManager().findFragmentById(R.id.projects_pane_list);
		final Bundle args = new Bundle();
		args.putBoolean(KEY_IS_SPLIT_SCREEN, mIsSplitScreen);
		if (frag instanceof ProjectsListFragment) {
			// ((ProjectsListFragment) frag).updateCurrentProject(mProjects.get(mCurrentProjectPosition).id);
			if (mIsSplitScreen) {
				final Fragment f = getSupportFragmentManager().findFragmentById(R.id.projects_pane_project);
				if (f != null) {
					getSupportFragmentManager().beginTransaction().remove(f).commit();
				}
			}
		} else if (frag instanceof ProjectFragment) {
			getSupportFragmentManager().beginTransaction().replace(R.id.projects_pane_list, ProjectsListFragment.newInstance(args)).commit();
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_IS_SPLIT_SCREEN, mIsSplitScreen);
	}
}
