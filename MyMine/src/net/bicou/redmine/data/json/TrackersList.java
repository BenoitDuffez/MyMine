package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 20/05/13.
 */
public class TrackersList extends AbsObjectList<Tracker> {
	public List<Tracker> trackers;

	@Override
	public void addObjects(List<Tracker> objects) {
		if (trackers == null) {
			trackers = new ArrayList<Tracker>();
		}
		trackers.addAll(objects);
	}

	@Override
	public int getSize() {
		return trackers == null ? 0 : trackers.size();
	}

	@Override
	public List<Tracker> getObjects() {
		return trackers;
	}
}
