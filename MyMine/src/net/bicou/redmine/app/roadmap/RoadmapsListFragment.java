package net.bicou.redmine.app.roadmap;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.util.L;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 28/05/13.
 */
public class RoadmapsListFragment extends SherlockListFragment {
	private List<Version> mList;
	RoadmapsAdapter mAdapter;
	RoadmapSelectionListener mListener;

	public interface RoadmapSelectionListener {
		public void onRoadmapSelected(Version version);
	}

	public interface CurrentProjectInfo {
		public Project getCurrentProject();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof RoadmapSelectionListener)) {
			throw new IllegalArgumentException("Activity " + activity.getClass().getName() + " must implement RoadmapSelectionListener");
		}
		mListener = (RoadmapSelectionListener) activity;
		mList = new ArrayList<Version>();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		mListener.onRoadmapSelected(mAdapter.getItem(position));
	}

	public static RoadmapsListFragment newInstance(Bundle args) {
		RoadmapsListFragment frag = new RoadmapsListFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_roadmaps_list, container, false);

		mAdapter = new RoadmapsAdapter(getActivity(), mList);
		setListAdapter(mAdapter);

		updateRoadmapsList();

		return v;
	}

	public void updateRoadmapsList() {
		final CurrentProjectInfo info = (CurrentProjectInfo) getActivity();
		mList.clear();

		if (info == null || info.getCurrentProject() == null) {
			L.e("shouldn't happen", null);
		} else {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... contexts) {
					VersionsDbAdapter db = new VersionsDbAdapter(getActivity());
					db.open();
					mList.addAll(db.selectAll(info.getCurrentProject().server, info.getCurrentProject()));
					db.close();
					return null;
				}

				@Override
				protected void onPostExecute(Void aVoid) {
					mAdapter.notifyDataSetChanged();
				}
			}.execute();
		}
	}

	private static class RoadmapListItemViewsHolder {
		TextView name, date;
	}

	private static class RoadmapsAdapter extends BaseAdapter {
		List<Version> mItems;
		LayoutInflater mLayoutInflater;
		DateFormat mDateFormat;
		Context mContext;

		public RoadmapsAdapter(Context ctx, List<Version> items) {
			mItems = items;
			mContext = ctx;
			mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mDateFormat = android.text.format.DateFormat.getLongDateFormat(ctx);
		}

		@Override
		public long getItemId(int i) {
			return 0;
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Version getItem(int i) {
			if (i >= 0 && i < getCount()) {
				return mItems.get(i);
			}
			return null;
		}

		@Override
		public View getView(int i, View convertView, ViewGroup viewGroup) {
			View v;
			RoadmapListItemViewsHolder holder;

			if (convertView == null) {
				v = mLayoutInflater.inflate(R.layout.roadmap_list_item, viewGroup, false);
				holder = new RoadmapListItemViewsHolder();
				holder.name = (TextView) v.findViewById(R.id.roadmap_list_item_name);
				holder.date = (TextView) v.findViewById(R.id.roadmap_list_item_date);
				v.setTag(holder);
			} else {
				v = convertView;
				holder = (RoadmapListItemViewsHolder) v.getTag();
			}

			Version item = getItem(i);
			holder.name.setText(item.name);
			if (item.due_date == null || item.due_date.getTimeInMillis() < 1000) {
				holder.date.setVisibility(View.GONE);
			} else {
				holder.date.setVisibility(View.VISIBLE);
				String date = mDateFormat.format(item.due_date.getTime());
				holder.date.setText(mContext.getString(R.string.roadmap_list_item_due_date, date));
			}

			return v;
		}
	}
}

