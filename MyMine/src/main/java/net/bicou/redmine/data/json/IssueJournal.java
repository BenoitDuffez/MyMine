package net.bicou.redmine.data.json;

import java.util.List;

/**
 * Journal container
 */
public class IssueJournal extends IssueHistory<Journal> {
	public List<Journal> journals;

	public int size() {
		int nb = 0;
		if (journals != null) {
			nb += journals.size();
		}
		return nb;
	}

	public Journal getItem(int position) {
		return journals == null ? null : journals.get(position);
	}
}
