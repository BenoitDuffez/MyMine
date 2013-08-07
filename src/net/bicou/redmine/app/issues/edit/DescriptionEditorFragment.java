package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Created by bicou on 06/08/13.
 */
public class DescriptionEditorFragment extends SherlockDialogFragment {
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
		return new DescriptionEditorDialog(getActivity(), description);
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		if (activity instanceof DescriptionChangeListener) {
			((DescriptionEditorDialog) getDialog()).setListener((DescriptionChangeListener) activity);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		((DescriptionEditorDialog) getDialog()).setListener(null);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(EditIssueFragment.KEY_ISSUE_DESCRIPTION, ((DescriptionEditorDialog) getDialog()).getDescription());
	}
}
