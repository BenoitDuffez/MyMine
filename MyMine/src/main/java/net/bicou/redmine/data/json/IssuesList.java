package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

public class IssuesList extends AbsObjectList<Issue> {
	public List<Issue> issues;

	long projectId;

	@Override
	public void addObjects(final List<Issue> objects) {
		if (issues == null) {
			issues = new ArrayList<Issue>();
		}
		issues.addAll(objects);
	}

	@Override
	public List<Issue> getObjects() {
		return issues;
	}

	@Override
	public int getSize() {
		return issues == null ? 0 : issues.size();
	}
}
