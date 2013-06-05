package net.bicou.redmine.app.issues;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter.OrderColumn;
import net.bicou.redmine.app.issues.IssuesOrderingPickColumnFragment.ColumnPickSelectionListener;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.util.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IssuesOrderColumnsAdapter extends ArrayAdapter<OrderColumn> implements OnClickListener {
	public static final String[] AVAILABLE_COLUMNS = new String[] {
			IssuesDbAdapter.KEY_ID,
			IssuesDbAdapter.KEY_PROJECT_ID,
			IssuesDbAdapter.KEY_TRACKER_ID,
			IssuesDbAdapter.KEY_PRIORITY_ID,
			IssuesDbAdapter.KEY_STATUS_ID,
			IssuesDbAdapter.KEY_AUTHOR_ID,
			IssuesDbAdapter.KEY_SUBJECT,
			IssuesDbAdapter.KEY_DESCRIPTION,
			IssuesDbAdapter.KEY_START_DATE,
			IssuesDbAdapter.KEY_DONE_RATIO,
			IssuesDbAdapter.KEY_CREATED_ON,
			IssuesDbAdapter.KEY_UPDATED_ON,
			IssuesDbAdapter.KEY_DUE_DATE,
			IssuesDbAdapter.KEY_FIXED_VERSION_ID,
			IssuesDbAdapter.KEY_CATEGORY_ID,
			IssuesDbAdapter.KEY_ASSIGNED_TO_ID,
			IssuesDbAdapter.KEY_ESTIMATED_HOURS,
			IssuesDbAdapter.KEY_SPENT_HOURS,
	};

	@SuppressWarnings("serial")
	static final HashMap<String, Integer> AVAILABLE_COLUMNS_NAMES = new HashMap<String, Integer>() {
		{
			put(IssuesDbAdapter.KEY_ID, R.string.issue_id);
			put(IssuesDbAdapter.KEY_PROJECT_ID, R.string.issue_project);
			put(IssuesDbAdapter.KEY_TRACKER_ID, R.string.issue_tracker);
			put(IssuesDbAdapter.KEY_PRIORITY_ID, R.string.issue_priority);
			put(IssuesDbAdapter.KEY_STATUS_ID, R.string.issue_status);
			put(IssuesDbAdapter.KEY_AUTHOR_ID, R.string.issue_author);
			put(IssuesDbAdapter.KEY_SUBJECT, R.string.issue_subject);
			put(IssuesDbAdapter.KEY_DESCRIPTION, R.string.issue_description);
			put(IssuesDbAdapter.KEY_START_DATE, R.string.issue_start_date);
			put(IssuesDbAdapter.KEY_DONE_RATIO, R.string.issue_percent_done);
			put(IssuesDbAdapter.KEY_CREATED_ON, R.string.issue_creation_date);
			put(IssuesDbAdapter.KEY_UPDATED_ON, R.string.issue_update_date);
			put(IssuesDbAdapter.KEY_DUE_DATE, R.string.issue_due_date);
			put(IssuesDbAdapter.KEY_FIXED_VERSION_ID, R.string.issue_target_version);
			put(IssuesDbAdapter.KEY_CATEGORY_ID, R.string.issue_category);
			put(IssuesDbAdapter.KEY_ASSIGNED_TO_ID, R.string.issue_assignee);
			put(IssuesDbAdapter.KEY_ESTIMATED_HOURS, R.string.issue_estimated_hours);
			put(IssuesDbAdapter.KEY_SPENT_HOURS, R.string.issue_spent_time);
		}
	};

	public static class OrderColumn implements Parcelable {
		public String key;
		public boolean isAscending;

		@SuppressWarnings("serial")
		public static final ArrayList<OrderColumn> getDefaultOrder() {
			return new ArrayList<IssuesOrderColumnsAdapter.OrderColumn>() {
				{
					add(new OrderColumn(IssuesDbAdapter.KEY_ID, false));
				}
			};
		}

		public OrderColumn(final String k, final boolean asc) {
			key = k;
			isAscending = asc;
		}

		public OrderColumn(final Parcel in) {
			key = in.readString();
			isAscending = in.readInt() > 0;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(final Parcel dest, final int flags) {
			dest.writeString(key);
			dest.writeInt(isAscending ? 1 : 0);
		}

		public static final Parcelable.Creator<OrderColumn> CREATOR = new Parcelable.Creator<OrderColumn>() {
			@Override
			public OrderColumn createFromParcel(final Parcel in) {
				return new OrderColumn(in);
			}

			@Override
			public OrderColumn[] newArray(final int size) {
				return new OrderColumn[size];
			}
		};

		@Override
		public String toString() {
			return "OrderColumn { key: " + key + ", isAscending: " + isAscending + " }";
		}
	}

	static class OrderColumnViewsHolder {
		public TextView key;
		public ToggleButton order;
	}

	LayoutInflater mInflater;
	List<OrderColumn> mData;

	public IssuesOrderColumnsAdapter(final Context context, final List<OrderColumn> items) {
		super(context, 0, 0, items);
		mData = items;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View v;
		final OrderColumnViewsHolder holder;

		if (convertView == null) {
			v = mInflater.inflate(R.layout.issues_order_column, parent, false);
			holder = new OrderColumnViewsHolder();
			holder.key = (TextView) v.findViewById(R.id.issues_order_column_name);
			holder.order = (ToggleButton) v.findViewById(R.id.issues_order_column_direction);
			v.setTag(R.id.issues_order_column_handle, holder);
		} else {
			v = convertView;
			holder = (OrderColumnViewsHolder) v.getTag(R.id.issues_order_column_handle);
		}

		final OrderColumn item = getItem(position);

		holder.key.setText(getContext().getString(AVAILABLE_COLUMNS_NAMES.get(item.key)));
		holder.key.setOnClickListener(this);
		holder.key.setTag(R.id.issues_order_column_name, item);

		holder.order.setChecked(item.isAscending);
		holder.order.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				item.isAscending = !item.isAscending;
			}
		});

		return v;
	}

	public void addNext() {
		String nextCol = null;
		boolean found;
		for (final String col : AVAILABLE_COLUMNS) {
			found = false;
			for (int i = 0; i < getCount(); i++) {
				if (col.equals(getItem(i).key)) {
					found = true;
					break;
				}
			}

			if (!found) {
				nextCol = col;
				break;
			}
		}

		if (nextCol != null) {
			add(new OrderColumn(nextCol, true));
		}
	}

	public ArrayList<OrderColumn> getColumns() {
		return (ArrayList<OrderColumn>) mData;
	}

	@Override
	public void onClick(final View v) {
		final OrderColumn item = (OrderColumn) v.getTag(R.id.issues_order_column_name);
		L.d("clicked on " + item);
		final HashMap<String, Integer> cols = new HashMap<String, Integer>(AVAILABLE_COLUMNS_NAMES);
		for (final OrderColumn col : getColumns()) {
			if (cols.containsKey(col)) {
				cols.remove(cols.get(col));
			}
		}
		final SherlockFragmentActivity act = (SherlockFragmentActivity) getContext();
		final IssuesOrderingPickColumnFragment pickColumn = IssuesOrderingPickColumnFragment.newInstance(cols);
		pickColumn.setColumnPickSelectionListener(new ColumnPickSelectionListener() {
			@Override
			public void onColumnSelected(final String key) {
				item.key = key;
				notifyDataSetChanged();
			}
		});
		pickColumn.show(act.getSupportFragmentManager(), "pick_column");
	}
}
