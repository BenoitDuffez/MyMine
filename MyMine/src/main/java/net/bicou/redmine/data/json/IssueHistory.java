package net.bicou.redmine.data.json;

/**
 * Base class for issue history items holder. Can be either journal or revisions
 */
public abstract class IssueHistory<Type> {
	public abstract int size();

	public abstract Type getItem(int position);
}
