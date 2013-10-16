package net.bicou.redmine.app.issues.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.BasicSpinnerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 06/08/13.
 */
class ServerProjectPickerDialog extends AlertDialog implements DialogInterface.OnClickListener {
	ServerProjectPickerFragment.ServerProjectSelectionListener mListener;
	Spinner mServerSelector, mProjectSelector;

	public ServerProjectPickerDialog(final Context context, final Server server, final Project project) {
		super(context);

		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setIcon(0);
		setTitle(context.getString(R.string.issue_edit_description_dialog_title));

		View view = getLayoutInflater().inflate(R.layout.frag_issue_add_select_server_project, null);
		setView(view);
		setTitle(context.getString(R.string.issue_add_server_project_picker_dialog_title));

		ServersDbAdapter db = new ServersDbAdapter(context);
		db.open();
		List<Server> servers = db.selectAll();
		db.close();

		BasicSpinnerAdapter<Server> serversAdapter = new BasicSpinnerAdapter<Server>(context, servers) {
			@Override
			public String getText(final Server item) {
				return item.serverUrl;
			}
		};
		final ArrayList<Project> projects = new ArrayList<Project>();
		final BasicSpinnerAdapter<Project> projectsAdapter = new BasicSpinnerAdapter<Project>(context, projects) {
			@Override
			public String getText(final Project item) {
				return item.name;
			}
		};

		mProjectSelector = (Spinner) view.findViewById(R.id.issue_add_project_picker);
		mProjectSelector.setAdapter(projectsAdapter);

		mServerSelector = (Spinner) view.findViewById(R.id.issue_add_server_picker);
		mServerSelector.setAdapter(serversAdapter);
		mServerSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				ProjectsDbAdapter db = new ProjectsDbAdapter(context);
				db.open();
				projectsAdapter.clear();
				int i = 0, sel = -1;
				for (Project p : db.selectAll((Server) parent.getAdapter().getItem(position))) {
					projectsAdapter.add(p);
					if (project != null && project.id == p.id) {
						sel = i;
					}
					i++;
				}
				db.close();
				projectsAdapter.notifyDataSetChanged();
				if (sel > 0) {
					mProjectSelector.setSelection(sel);
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				projectsAdapter.clear();
				projectsAdapter.notifyDataSetChanged();
			}
		});

		if (servers.size() > 1) {
			int sel = 0;
			for (Server s : servers) {
				if (s != null && server != null &&fix  s.rowId == server.rowId) {
					mServerSelector.setSelection(sel);
					break;
				}
				sel++;
			}
		}
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (which == BUTTON_POSITIVE && mListener != null) {
			Server server = (Server) mServerSelector.getSelectedItem();
			Project project = (Project) mProjectSelector.getSelectedItem();
			mListener.onServerProjectPicked(server, project);
		}
	}

	public void setListener(ServerProjectPickerFragment.ServerProjectSelectionListener listener) {
		mListener = listener;
	}

	public Server getServer() {
		return (Server) mServerSelector.getSelectedItem();
	}

	public Project getProject() {
		return (Project) mProjectSelector.getSelectedItem();
	}
}
