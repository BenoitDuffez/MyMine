package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 21/05/13.
 */
public class IssueCategoriesList extends AbsObjectList<IssueCategory> {
	public List<IssueCategory> issue_categories;

	@Override
	public int getSize() {
		return issue_categories == null ? 0 : issue_categories.size();
	}

	@Override
	public void addObjects(List<IssueCategory> objects) {
		if (issue_categories == null) {
			issue_categories = new ArrayList<IssueCategory>();
		}
		issue_categories.addAll(objects);
	}

	@Override
	public List<IssueCategory> getObjects() {
		return issue_categories;
	}
}
