package net.bicou.redmine.app.projects;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ProjectsActivity extends SherlockFragmentActivity {
	/** Whether the screen is split into a list + an item. Likely the case on tablets and/or in landscape orientation */
	public static final String KEY_IS_SPLIT_SCREEN = "net.bicou.redmine.projects.SplitScreen";
	//TODO	boolean mIsSplitScreen;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle args = new Bundle();

		// Setup fragments
		if (savedInstanceState == null) {
			// Setup list view
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, ProjectsListFragment.newInstance(args)).commit();
		} else if (savedInstanceState.containsKey(ProjectFragment.KEY_PROJECT_JSON)) {
			// Setup content view, if possible
		}

		// Screen rotation on 7" tablets
		//		if (savedInstanceState != null && mIsSplitScreen != savedInstanceState.getBoolean(KEY_IS_SPLIT_SCREEN)) {
		//			final Fragment f = getSupportFragmentManager().findFragmentById(android.R.id.content);
		//			if (f != null && f instanceof ProjectsListFragment) {
		//				((ProjectsListFragment) f).updateSplitScreenState(mIsSplitScreen);
		//			}
		//		}
	}

	@Override
	public void onResume() {
		super.onResume();
		//		final Fragment frag = getSupportFragmentManager().findFragmentById(android.R.id.content);
		//		final Bundle args = new Bundle();
		//		if (frag instanceof ProjectsListFragment) {
		//			//			((ProjectsListFragment) frag).updateCurrentProject(mProjects.get(mCurrentProjectPosition).id);
		//			//			if (mIsSplitScreen) {
		//			//				final Fragment f = getSupportFragmentManager().findFragmentById(R.id.projects_pane_project);
		//			//				if (f != null) {
		//			//					getSupportFragmentManager().beginTransaction().remove(f).commit();
		//			//				}
		//			//			}
		//
		//		} else if (frag instanceof ProjectFragment) {
		//			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, ProjectsListFragment.newInstance(args)).commit();
		//		}
	}
}
