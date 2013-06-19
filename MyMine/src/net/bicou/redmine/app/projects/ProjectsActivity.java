package net.bicou.redmine.app.projects;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import net.bicou.android.splitscreen.SplitActivity;

public class ProjectsActivity extends SplitActivity<ProjectsListFragment, ProjectFragment> {
	@Override
	protected ProjectsListFragment createMainFragment(Bundle args) {
		return ProjectsListFragment.newInstance(args);
	}

	@Override
	protected ProjectFragment createContentFragment(Bundle args) {
		return ProjectFragment.newInstance(args);
	}

	@Override
	protected Fragment createEmptyFragment(Bundle args) {
		return new ProjectEmptyFragment();
	}

	private static class ProjectEmptyFragment extends SherlockFragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			TextView tv = new TextView(inflater.getContext());
			tv.setText("Empty view!");
			return tv;
		}
	}
}
