package net.bicou.redmine.app.issues.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import net.bicou.redmine.R;

/**
 * Created by bicou on 06/08/13.
 */
class DescriptionEditorDialog extends AlertDialog implements DialogInterface.OnClickListener {
	String mDescription;
	EditText mDescriptionEditor;
	DescriptionEditorFragment.DescriptionChangeListener mListener;

	DescriptionEditorDialog(final Context context, String description) {
		super(context);
		mDescription = description;

		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setIcon(0);
		setTitle(context.getString(R.string.issue_edit_description_dialog_title));

		View view = getLayoutInflater().inflate(R.layout.frag_issue_edit_description, null);
		setView(view);

		mDescriptionEditor = (EditText) view.findViewById(R.id.issue_edit_description);
		mDescriptionEditor.setText(mDescription);
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (which == BUTTON_POSITIVE && mListener != null) {
			mListener.onDescriptionChanged(mDescriptionEditor.getText() == null ? "" : mDescriptionEditor.getText().toString());
		}
	}

	public String getDescription() {
		return mDescriptionEditor.getText() == null ? null : mDescriptionEditor.getText().toString();
	}

	public void setListener(DescriptionEditorFragment.DescriptionChangeListener listener) {
		mListener = listener;
	}
}
