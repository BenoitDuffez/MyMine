package net.bicou.redmine.app.projects;

import java.text.DateFormat;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.gson.Gson;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ProjectFragment extends SherlockFragment {
	public static final String KEY_PROJECT_JSON = "net.bicou.redmine.Project";
	private Project mProject;
	TextView mUpdatedOn, mCreatedOn, mTitle, mDescription, mServer, mParent;

	public static ProjectFragment newInstance(final Bundle args) {
		final ProjectFragment f = new ProjectFragment();
		String log = "args: ";
		for (final String key : args.keySet()) {
			log += key + "=" + (args.get(key) == null ? "null" : "not null") + ", ";
		}
		L.d(log);
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_project_overview, container, false);
		L.d("");

		mCreatedOn = (TextView) v.findViewById(R.id.project_overview_created_on);
		mUpdatedOn = (TextView) v.findViewById(R.id.project_overview_updated_on);
		mServer = (TextView) v.findViewById(R.id.project_overview_server);
		mTitle = (TextView) v.findViewById(R.id.project_overview_title);
		mParent = (TextView) v.findViewById(R.id.project_overview_parent);
		mDescription = (TextView) v.findViewById(R.id.project_overview_description);

		final Bundle args = getArguments();
		if (savedInstanceState == null) {
			// Load project
			final long serverId = args.getLong(Constants.KEY_SERVER_ID);
			final ProjectsDbAdapter db = new ProjectsDbAdapter(getActivity());
			db.open();
			mProject = db.select(serverId, args.getLong(Constants.KEY_PROJECT_ID), null);
			db.close();
		} else {
			mProject = new Gson().fromJson(savedInstanceState.getString(KEY_PROJECT_JSON), Project.class);
		}

		return v;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mProject != null) {
			final String json = new Gson().toJson(mProject);
			outState.putString(KEY_PROJECT_JSON, json);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getActivity() != null && mProject != null) {
			getSherlockActivity().getSupportActionBar().setTitle(mProject.name);
			mTitle.setText(mProject.name);
			mCreatedOn.setText(Html.fromHtml(getString(R.string.project_created_on, DateFormat.getDateInstance(DateFormat.LONG).format(mProject.created_on.getTime()))));
			mUpdatedOn.setText(Html.fromHtml(getString(R.string.project_updated_on, DateFormat.getDateInstance(DateFormat.LONG).format(mProject.updated_on.getTime()))));
			mServer.setText(Html.fromHtml(getString(R.string.project_server, mProject.server.serverUrl)));
			if (mProject.parent == null || mProject.parent.id <= 0) {
				mParent.setVisibility(View.GONE);
			} else {
				mParent.setText(Html.fromHtml(getString(R.string.project_parent, mProject.parent.name)));
			}
			if (!TextUtils.isEmpty(mProject.description)) {
				mDescription.setText(Html.fromHtml(Util.htmlFromTextile(mProject.description)));
			}
		}
	}
}
