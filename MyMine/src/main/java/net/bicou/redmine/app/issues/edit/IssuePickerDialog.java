package net.bicou.redmine.app.issues.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesListCursorLoader;
import net.bicou.redmine.app.issues.IssuesListFilter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.util.L;

/**
 * Used to select an issue when a file was just uploaded.
 * Created by bicou on 06/08/13.
 */
public class IssuePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	private static final int ACTION_LOAD_ISSUES = 0;
	private IssuesCursorAdapter mAdapter;
	IssuePickerFragment.IssueSelectionListener mListener;
	Spinner mIssueSelector;
	private IssuesDbAdapter mHelper;

	public IssuePickerDialog(Context context) {
		super(context);

		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setIcon(0);

		View view = getLayoutInflater().inflate(R.layout.frag_issue_attn_select_issue, null);
		if (view == null) {
			L.e("Can't inflate view", null);
			return;
		}
		setView(view);
		setTitle(context.getString(R.string.issue_attn_select_issue_dialog_title));

		mIssueSelector = (Spinner) view.findViewById(R.id.issue_attn_issue_picker);
		EditText search = (EditText) view.findViewById(R.id.issue_attn_search);
		search.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable v) {
				Bundle args = new Bundle();
				String searchTerms = v != null && !TextUtils.isEmpty(v) ? v.toString() : "";
				new IssuesListFilter(searchTerms).saveTo(args);
				((FragmentActivity) getOwnerActivity()).getSupportLoaderManager().restartLoader(ACTION_LOAD_ISSUES, args, IssuePickerDialog.this);
			}
		});
	}

	private IssuesDbAdapter getHelper() {
		if (mHelper == null) {
			mHelper = new IssuesDbAdapter(getContext());
			mHelper.open();
		}
		return mHelper;
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		getHelper();
		mAdapter = new IssuesCursorAdapter(getOwnerActivity());
		mIssueSelector.setAdapter(mAdapter);
		FragmentActivity ownerActivity = (FragmentActivity) getOwnerActivity();
		LoaderManager supportLoaderManager = ownerActivity.getSupportLoaderManager();
		supportLoaderManager.initLoader(ACTION_LOAD_ISSUES, null, this);
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mHelper != null) {
			mHelper.close();
			mHelper = null;
		}
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (which == BUTTON_POSITIVE && mListener != null) {
			mListener.onIssuePicked(getIssueId());
		}
	}

	public void setListener(IssuePickerFragment.IssueSelectionListener listener) {
		mListener = listener;
	}

	public long getIssueId() {
		return mIssueSelector.getSelectedItemId();
	}

	class IssuesCursorAdapter extends CursorAdapter {
		class ViewsHolder {
			TextView subject;
		}

		public IssuesCursorAdapter(Context context) {
			super(context, null, false);
			// do something with args
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
			if (v == null) {
				L.e("Can't inflate view", null);
				return null;
			}

			ViewsHolder holder = new ViewsHolder();
			holder.subject = (TextView) v.findViewById(android.R.id.text1);
			v.setTag(holder);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewsHolder holder = (ViewsHolder) view.getTag();
			String subject = cursor.getString(cursor.getColumnIndex(IssuesDbAdapter.KEY_SUBJECT));
			holder.subject.setText(subject);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new IssuesListCursorLoader(getOwnerActivity(), getHelper(), args);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
