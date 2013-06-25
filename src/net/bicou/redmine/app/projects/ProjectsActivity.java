package net.bicou.redmine.app.projects;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.android.splitscreen.SplitActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.app.misc.EmptyFragment;

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
		return EmptyFragment.newInstance(R.drawable.projects_empty_fragment);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
