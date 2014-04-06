package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import net.bicou.redmine.Constants;
import net.bicou.redmine.app.ga.TrackedDialogFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;

/**
 * Server or Server+Project picker dialog fragment
 */
public class ServerProjectPickerFragment extends TrackedDialogFragment {
	private static final String EXTRA_DIALOG_CONTENTS = "net.bicou.redmine.app.issues.edit.ServerProjectDialogContents";
	ServerProjectPickerDialog.DesiredSelection mDesiredSelection;

	public interface ServerProjectSelectionListener {
		public void onServerProjectPicked(ServerProjectPickerDialog.DesiredSelection desiredSelection, Server server, Project project);
	}

	public static ServerProjectPickerFragment newInstance(ServerProjectPickerDialog.DesiredSelection contents) {
		ServerProjectPickerFragment fragment = new ServerProjectPickerFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_DIALOG_CONTENTS, contents.name());
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Server server = null;
		Project project = null;
		mDesiredSelection = ServerProjectPickerDialog.DesiredSelection.valueOf(getArguments().getString(EXTRA_DIALOG_CONTENTS));

		if (savedInstanceState != null) {
			server = savedInstanceState.getParcelable(Constants.KEY_SERVER);
			project = savedInstanceState.getParcelable(Constants.KEY_PROJECT);
		}

		return new ServerProjectPickerDialog(getActivity(), mDesiredSelection, server, project, (ServerProjectSelectionListener) getActivity());
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		setDialogListener((ServerProjectSelectionListener) activity);
	}

	private void setDialogListener(ServerProjectSelectionListener listener) {
		if (getDialog() != null) {
			((ServerProjectPickerDialog) getDialog()).setListener(listener);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		setDialogListener(null);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(Constants.KEY_SERVER, ((ServerProjectPickerDialog) getDialog()).getServer());
		outState.putParcelable(Constants.KEY_PROJECT, ((ServerProjectPickerDialog) getDialog()).getProject());
	}
}
