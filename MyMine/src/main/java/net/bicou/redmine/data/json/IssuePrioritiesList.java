package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 21/05/13.
 */
public class IssuePrioritiesList extends AbsObjectList<IssuePriority> {
	public List<IssuePriority> issue_priorities;

	@Override
	public int getSize() {
		return issue_priorities == null ? 0 : issue_priorities.size();
	}

	@Override
	public void addObjects(List<IssuePriority> objects) {
		if (issue_priorities == null) {
			issue_priorities = new ArrayList<IssuePriority>();
		}
		issue_priorities.addAll(objects);
	}

	@Override
	public List<IssuePriority> getObjects() {
		return issue_priorities;
	}
}
