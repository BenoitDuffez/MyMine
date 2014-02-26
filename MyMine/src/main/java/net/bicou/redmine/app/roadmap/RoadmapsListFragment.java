package net.bicou.redmine.app.roadmap;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.widget.IssuesListRelativeLayout;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 28/05/13.
 */
public class RoadmapsListFragment extends TrackedListFragment {
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
	public void onDetach() {
		super.onDetach();
		mListener = null;
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
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateRoadmap();
	}

	public void updateRoadmap() {
		AsyncTaskFragment.runTask((ActionBarActivity) getActivity(), RoadmapActivity.ACTION_LOAD_ROADMAP, null);
	}

	public static List<Version> getRoadmap(Context ctx, Server server, Project project) {
		List<Version> roadmap = new ArrayList<Version>();
		VersionsDbAdapter db = new VersionsDbAdapter(ctx);
		db.open();
		roadmap.addAll(db.selectAll(server, project));
		db.close();
		return roadmap;
	}

	public void onRoadmapLoaded(List<Version> roadmap) {
		mList.clear();
		if (roadmap != null) {
			for (Version v : roadmap) {
				mList.add(v);
			}
		}
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private static class RoadmapListItemViewsHolder {
		TextView name, date;
		IssuesListRelativeLayout layout;
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
				holder.layout = (IssuesListRelativeLayout) v.findViewById(R.id.roadmap_list_item_layout);
				holder.name = (TextView) v.findViewById(R.id.roadmap_list_item_name);
				holder.date = (TextView) v.findViewById(R.id.roadmap_list_item_date);
				v.setTag(holder);
			} else {
				v = convertView;
				holder = (RoadmapListItemViewsHolder) v.getTag();
			}

			Version item = getItem(i);
			holder.layout.setIsClosed(item.status == Version.VersionStatus.CLOSED);
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

