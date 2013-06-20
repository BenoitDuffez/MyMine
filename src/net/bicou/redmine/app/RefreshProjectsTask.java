package net.bicou.redmine.app;

import android.content.Context;
import android.os.AsyncTask;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.util.L;

import java.util.List;

/**
 * Created by bicou on 17/06/13.
 */
public class RefreshProjectsTask extends AsyncTask<Void, Void, List<Project>> {
	public interface ProjectsLoadCallbacks {
		void onProjectsLoaded(List<Project> projectList);
	}

	private ProjectsLoadCallbacks mCallbacks;
	Context mContext;

	public RefreshProjectsTask(Context ctx, ProjectsLoadCallbacks callbacks) {
		this.mCallbacks = callbacks;
		mContext = ctx;
	}

	@Override
	protected List<Project> doInBackground(final Void... params) {
		L.d("");
		final ProjectsDbAdapter db = new ProjectsDbAdapter(mContext);
		db.open();
		List<Project> projects = db.selectAll();
		db.close();

		return projects;
	}

	@Override
	protected void onPostExecute(final List<Project> projects) {
		L.d("");
		mCallbacks.onProjectsLoaded(projects);
	}
}
