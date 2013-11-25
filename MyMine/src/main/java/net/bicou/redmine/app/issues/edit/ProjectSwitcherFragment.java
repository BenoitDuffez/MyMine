package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import net.bicou.redmine.app.ga.TrackedDialogFragment;

/**
 * Created by bicou on 06/08/13.
 */
public class ProjectSwitcherFragment extends TrackedDialogFragment {
	ProjectChangeListener mListener;

	public interface ProjectChangeListener {
		public void onProjectChanged(long newProjectId);
	}

	public static ProjectSwitcherFragment newInstance(Bundle args) {
		ProjectSwitcherFragment frag = new ProjectSwitcherFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		long projectId;
		if (savedInstanceState == null) {
			projectId = getArguments().getLong(EditIssueFragment.KEY_ISSUE_PROJECT_ID);
		} else {
			projectId = savedInstanceState.getLong(EditIssueFragment.KEY_ISSUE_PROJECT_ID);
		}
		ProjectSwitcherDialog dialog = new ProjectSwitcherDialog(getActivity(), projectId);
		if (mListener != null) {
			dialog.setListener(mListener);
		} else if (getActivity() != null && getActivity() instanceof ProjectChangeListener) {
			dialog.setListener((ProjectChangeListener) getActivity());
		} else {
			throw new IllegalStateException("Can't bind listener!");
		}
		return dialog;
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ProjectChangeListener) {
			mListener = (ProjectChangeListener) activity;
			if (getDialog() != null) {
				((ProjectSwitcherDialog) getDialog()).setListener(mListener);
			}
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (getDialog() != null) {
			((DescriptionEditorDialog) getDialog()).setListener(null);
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(EditIssueFragment.KEY_ISSUE_PROJECT_ID, ((ProjectSwitcherDialog) getDialog()).getProjectId());
	}
}
