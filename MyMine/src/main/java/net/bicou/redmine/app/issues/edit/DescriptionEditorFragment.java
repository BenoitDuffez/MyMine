package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import net.bicou.redmine.app.ga.TrackedDialogFragment;

/**
 * Created by bicou on 06/08/13.
 */
public class DescriptionEditorFragment extends TrackedDialogFragment {
	DescriptionChangeListener mListener;

	public interface DescriptionChangeListener {
		public void onDescriptionChanged(String newDescription);
	}

	public static DescriptionEditorFragment newInstance(Bundle args) {
		DescriptionEditorFragment frag = new DescriptionEditorFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String description;
		if (savedInstanceState == null) {
			description = getArguments().getString(EditIssueFragment.KEY_ISSUE_DESCRIPTION);
		} else {
			description = savedInstanceState.getString(EditIssueFragment.KEY_ISSUE_DESCRIPTION);
		}
		DescriptionEditorDialog dialog = new DescriptionEditorDialog(getActivity(), description);
		if (mListener != null) {
			dialog.setListener(mListener);
		} else if (getActivity() != null && getActivity() instanceof DescriptionChangeListener) {
			dialog.setListener((DescriptionChangeListener) getActivity());
		} else {
			throw new IllegalStateException("Can't bind listener!");
		}
		return dialog;
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		if (activity instanceof DescriptionChangeListener) {
			mListener = (DescriptionChangeListener) activity;
			if (getDialog() != null) {
				((DescriptionEditorDialog) getDialog()).setListener(mListener);
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
		outState.putString(EditIssueFragment.KEY_ISSUE_DESCRIPTION, ((DescriptionEditorDialog) getDialog()).getDescription());
	}
}
