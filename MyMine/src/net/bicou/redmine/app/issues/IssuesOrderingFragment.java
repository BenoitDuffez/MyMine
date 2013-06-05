package net.bicou.redmine.app.issues;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.gson.reflect.TypeToken;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter.OrderColumn;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class IssuesOrderingFragment extends SherlockDialogFragment {
	IssuesOrderColumnsAdapter mAdapter;
	DragSortListView mDslv;
	DragSortController mController;
	public static final String KEY_COLUMNS_ORDER = "net.bicou.redmine.app.issues.ColumnsOrder";
	IssuesOrderSelectionListener mListener;

	public final static Type ORDER_TYPE = new TypeToken<ArrayList<OrderColumn>>() {
	}.getType();

	public static IssuesOrderingFragment newInstance(final ArrayList<OrderColumn> order) {
		final IssuesOrderingFragment frag = new IssuesOrderingFragment();
		final Bundle args = new Bundle();
		args.putParcelableArrayList(KEY_COLUMNS_ORDER, order);
		frag.setArguments(args);
		return frag;
	}

	public interface IssuesOrderSelectionListener {
		public void onOrderColumnsSelected(ArrayList<OrderColumn> orderColumns);
	}

	public void setOrderSelectionListener(final IssuesOrderSelectionListener listener) {
		mListener = listener;
	}

	private final DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(final int from, final int to) {
			if (from != to) {
				final OrderColumn item = mAdapter.getItem(from);
				mAdapter.remove(item);
				mAdapter.insert(item, to);
			}
		}
	};

	private final DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
		@Override
		public void remove(final int which) {
			mAdapter.remove(mAdapter.getItem(which));
		}
	};

	/**
	 * Provide a custom DragSortController.
	 */
	public DragSortController buildController(final DragSortListView dslv) {
		final DragSortController controller = new DragSortController(dslv);
		controller.setRemoveEnabled(true);
		controller.setSortEnabled(true);
		controller.setDragHandleId(R.id.issues_order_column_handle);
		controller.setDragInitMode(DragSortController.ON_DOWN);
		controller.setRemoveMode(DragSortController.FLING_REMOVE);
		return controller;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(KEY_COLUMNS_ORDER, mAdapter.getColumns());
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final LayoutInflater inflater = getActivity().getLayoutInflater();

		List<OrderColumn> items;
		if (savedInstanceState != null) {
			items = savedInstanceState.getParcelableArrayList(KEY_COLUMNS_ORDER);
		} else if (getArguments().containsKey(KEY_COLUMNS_ORDER)) {
			items = getArguments().getParcelableArrayList(KEY_COLUMNS_ORDER);
		} else {
			items = OrderColumn.getDefaultOrder();
		}
		mAdapter = new IssuesOrderColumnsAdapter(getActivity(), items);

		final View v = inflater.inflate(R.layout.frag_issues_sort_order, null, false);

		mDslv = (DragSortListView) v.findViewById(R.id.issues_ordering_columns);
		mController = buildController(mDslv);
		mDslv.setFloatViewManager(mController);
		mDslv.setOnTouchListener(mController);
		mDslv.setDragEnabled(true);
		mDslv.setDropListener(onDrop);
		mDslv.setRemoveListener(onRemove);
		mDslv.setAdapter(mAdapter);

		v.findViewById(R.id.issues_ordering_add).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				mAdapter.addNext();
			}
		});

		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(v) //
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int id) {
						mListener.onOrderColumnsSelected(mAdapter.getColumns());
					}
				}) //
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int id) {
					}
				});

		return builder.create();
	}
}
