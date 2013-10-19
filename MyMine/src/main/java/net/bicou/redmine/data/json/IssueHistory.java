package net.bicou.redmine.data.json;

import java.util.List;

/**
 * Created by bicou on 21/05/13.
 */
public class IssueHistory {
	public List<Journal> journals;
	public List<ChangeSet> changesets;

	public int size() {
		int nb = 0;
		if (journals != null) {
			nb += journals.size();
		}
		if (changesets != null) {
			nb += changesets.size();
		}
		return nb;
	}

	public Object getItem(int position) {
		if (journals == null) {
			return null;
		}
		int nbJournals = journals.size();
		if (position < nbJournals) {
			return journals.get(position);
		} else if (changesets == null) {
			return null;
		} else {
			return changesets.get(position - nbJournals);
		}
	}
}
