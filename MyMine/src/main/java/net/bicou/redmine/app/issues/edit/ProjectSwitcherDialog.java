package net.bicou.redmine.app.issues.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;

import java.util.List;

/**
 * Created by bicou on 06/08/13.
 */
class ProjectSwitcherDialog extends AlertDialog implements DialogInterface.OnClickListener {
	private final Spinner mProjectsSpinner;
	ProjectSwitcherFragment.ProjectChangeListener mListener;

	private class ProjectsAdapter extends BaseAdapter {
		List<Project> mProjects;

		public ProjectsAdapter(List<Project> projects) {
			mProjects = projects;
		}

		@Override
		public int getCount() {
			return mProjects == null ? 0 : mProjects.size();
		}

		@Override
		public Project getItem(int position) {
			return position < 0 || position >= getCount() ? null : mProjects.get(position);
		}

		@Override
		public long getItemId(int position) {
			Project p = getItem(position);
			return p == null ? 0 : p.id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View v;

			if (convertView == null) {
				v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
			} else {
				v = convertView;
			}

			Project p = getItem(position);
			TextView tv = (TextView) v.findViewById(android.R.id.text1);
			tv.setText(p == null ? "N/A" : p.name);

			return v;
		}
	}

	ProjectSwitcherDialog(final Context context, long projectId) {
		super(context);

		setCanceledOnTouchOutside(false);
		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setIcon(0);
		setTitle(context.getString(R.string.issue_edit_description_dialog_title));

		ProjectsDbAdapter db = new ProjectsDbAdapter(getContext());
		db.open();
		final List<Project> projects = db.selectAll();
		ProjectsAdapter adapter = new ProjectsAdapter(projects);
		db.close();

		View view = getLayoutInflater().inflate(R.layout.frag_issue_edit_project, null);
		mProjectsSpinner = (Spinner) view.findViewById(R.id.issue_edit_project);
		mProjectsSpinner.setAdapter(adapter);
		int pos = 0;
		while (projects != null && projects.get(pos).id != projectId && pos < projects.size()) {
			pos++;
		}
		mProjectsSpinner.setSelection(pos);
		setView(view);
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (which == BUTTON_POSITIVE && mListener != null) {
			mListener.onProjectChanged(mProjectsSpinner.getSelectedItemId());
		}
	}

	public long getProjectId() {
		return mProjectsSpinner.getSelectedItemId();
	}

	public void setListener(ProjectSwitcherFragment.ProjectChangeListener listener) {
		mListener = listener;
	}
}
