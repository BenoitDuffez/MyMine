package net.bicou.redmine.data.json;

import java.util.List;

/**
 * Revisions
 */
public class IssueRevisions extends IssueHistory<ChangeSet> {
	public List<ChangeSet> changesets;

	public int size() {
		int nb = 0;
		if (changesets != null) {
			nb += changesets.size();
		}
		return nb;
	}

	public ChangeSet getItem(int position) {
		return changesets == null ? null : changesets.get(position);
	}
}
