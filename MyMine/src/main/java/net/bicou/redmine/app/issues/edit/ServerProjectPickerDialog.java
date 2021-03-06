package net.bicou.redmine.app.issues.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
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
 * Server or Server+Project picker dialog
 */
public class ServerProjectPickerDialog extends AlertDialog implements DialogInterface.OnClickListener {
	ServerProjectPickerFragment.ServerProjectSelectionListener mListener;
	Spinner mServerSelector, mProjectSelector;

	public enum DesiredSelection {
		SERVER,
		SERVER_PROJECT,
	}

	DesiredSelection mDesiredSelection = null;

	public ServerProjectPickerDialog(final Context context, DesiredSelection desiredSelection, final Server server, final Project project,
	                                 ServerProjectPickerFragment.ServerProjectSelectionListener listener) {
		super(context);
		mListener = listener;
		mDesiredSelection = desiredSelection;

		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setIcon(0);

		View view = getLayoutInflater().inflate(R.layout.frag_issue_add_select_server_project, null);
		setView(view);
		int resId = desiredSelection == DesiredSelection.SERVER_PROJECT ? //
				R.string.issue_add_server_project_picker_dialog_title : //
				R.string.issue_add_server_picker_dialog_title;
		setTitle(context.getString(resId));

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

		// Show/hide the project selector
		int visibility = mDesiredSelection == DesiredSelection.SERVER_PROJECT ? View.VISIBLE : View.GONE;
		mProjectSelector.setVisibility(visibility);
		view.findViewById(R.id.issue_add_project_picker_title).setVisibility(visibility);

		mServerSelector = (Spinner) view.findViewById(R.id.issue_add_server_picker);
		mServerSelector.setAdapter(serversAdapter);
		mServerSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
				ProjectsDbAdapter db = new ProjectsDbAdapter(context);
				db.open();
				projectsAdapter.clear();
				int i = 0, sel = -1;
				for (Project p : db.selectAll((Server) parent.getAdapter().getItem(position), ProjectsDbAdapter.KEY_IS_SYNC_BLOCKED + " != 1")) {
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
				if (s != null && server != null && s.rowId == server.rowId) {
					mServerSelector.setSelection(sel);
					break;
				}
				sel++;
			}
		} else if (servers.size() == 1 && desiredSelection == DesiredSelection.SERVER && mListener != null) {
			// If we just want to pick the server and there's only one, just do it
			mListener.onServerProjectPicked(desiredSelection, servers.get(0), null);

			// Ugly, but we can't #dissmis() right from the constructor...
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					dismiss();
				}
			}, 200);
		}
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (which == BUTTON_POSITIVE && mListener != null) {
			Server server = (Server) mServerSelector.getSelectedItem();
			Project project = (Project) mProjectSelector.getSelectedItem();
			mListener.onServerProjectPicked(mDesiredSelection, server, project);
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
