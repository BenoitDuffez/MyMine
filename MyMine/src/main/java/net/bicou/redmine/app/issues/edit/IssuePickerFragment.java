package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import net.bicou.redmine.app.ga.TrackedDialogFragment;

/**
 * Holder for the {@link net.bicou.redmine.app.issues.edit.IssuePickerDialog}
 */
public class IssuePickerFragment extends TrackedDialogFragment {
	public interface IssueSelectionListener {
		public void onServerProjectPicked(long issueId);
	}

	public static IssuePickerFragment newInstance() {
		return new IssuePickerFragment();
	}

	IssueSelectionListener mListener;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		IssuePickerDialog dialog = new IssuePickerDialog(getActivity());
		if (mListener != null) {
			dialog.setListener(mListener);
		} else if (getActivity() != null && getActivity() instanceof IssueSelectionListener) {
			dialog.setListener((IssueSelectionListener) getActivity());
		} else {
			throw new IllegalStateException("Can't bind listener!");
		}

		return dialog;
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		if (activity instanceof IssueSelectionListener) {
			mListener = (IssueSelectionListener) activity;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (getDialog() != null) {
			((ServerProjectPickerDialog) getDialog()).setListener(null);
		}
		mListener = null;
	}
}
