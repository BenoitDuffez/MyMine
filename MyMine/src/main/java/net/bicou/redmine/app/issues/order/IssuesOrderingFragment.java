package net.bicou.redmine.app.issues.order;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedDialogFragment;

public class IssuesOrderingFragment extends TrackedDialogFragment {
	IssuesOrderColumnsAdapter mAdapter;
	DragSortListView mDslv;
	DragSortController mController;
	IssuesOrderSelectionListener mListener;
	IssuesOrder mIssuesOrder;

	public static IssuesOrderingFragment newInstance(final IssuesOrder order) {
		final IssuesOrderingFragment frag = new IssuesOrderingFragment();
		final Bundle args = new Bundle();
		if (order != null) {
			order.saveTo(args);
		}
		frag.setArguments(args);
		return frag;
	}

	public interface IssuesOrderSelectionListener {
		public void onOrderColumnsSelected(IssuesOrder orderColumns);
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
		if (mIssuesOrder != null) {
			mIssuesOrder.saveTo(outState);
		}
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final LayoutInflater inflater = getActivity().getLayoutInflater();

		mIssuesOrder = IssuesOrder.fromBundle(savedInstanceState);
		mAdapter = new IssuesOrderColumnsAdapter(getActivity(), mIssuesOrder.getColumns());

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
						mListener.onOrderColumnsSelected(mAdapter.getIssuesOrder());
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
