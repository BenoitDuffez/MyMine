package net.bicou.redmine.app.issues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.bicou.redmine.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class IssuesOrderingPickColumnFragment extends SherlockDialogFragment {
	private static final String KEY_AVAILABLE_COLUMNS = "net.bicou.redmine.app.issues.OrderPickColumns";
	private static final String KEY_AVAILABLE_COLUMNS_NAMES = "net.bicou.redmine.app.issues.OrderPickColumnsNames";

	GridView mGridView;
	ColumnPickSelectionListener mListener;

	public interface ColumnPickSelectionListener {
		public void onColumnSelected(String key);
	}

	public static IssuesOrderingPickColumnFragment newInstance(final HashMap<String, Integer> availableColumns) {
		final ArrayList<Integer> colNames = new ArrayList<Integer>(availableColumns.values());
		final String[] colsArray = availableColumns.keySet().toArray(new String[availableColumns.size()]);
		final ArrayList<String> colsList = new ArrayList<String>(Arrays.asList(colsArray));

		final Bundle args = new Bundle();
		args.putStringArrayList(KEY_AVAILABLE_COLUMNS, colsList);
		args.putIntegerArrayList(KEY_AVAILABLE_COLUMNS_NAMES, colNames);

		final IssuesOrderingPickColumnFragment frag = new IssuesOrderingPickColumnFragment();
		frag.setArguments(args);

		return frag;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final List<String> cols = getArguments().getStringArrayList(KEY_AVAILABLE_COLUMNS);
		final List<Integer> namesResIds = getArguments().getIntegerArrayList(KEY_AVAILABLE_COLUMNS_NAMES);
		final List<String> colNames = new ArrayList<String>(namesResIds.size());
		for (final Integer resId : namesResIds) {
			colNames.add(getString(resId));
		}

		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View v = inflater.inflate(R.layout.frag_issues_order_pick_column, null, false);

		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, colNames);
		mGridView = (GridView) v.findViewById(R.id.issues_order_pick_column_grid);
		mGridView.setAdapter(adapter);

		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				mListener.onColumnSelected(cols.get(position));
				IssuesOrderingPickColumnFragment.this.getDialog().dismiss();
			}
		});

		return new AlertDialog.Builder(getActivity()).setView(v).create();
	}

	public void setColumnPickSelectionListener(final ColumnPickSelectionListener listener) {
		mListener = listener;
	}
}
