package net.bicou.redmine.app.issues;

import android.os.Bundle;

public class IssuesListFilter {
	public static final String KEY_FILTER_SERVER_ID = "net.bicou.redmine.app.issues.IssueListFilterServerId";
	public static final String KEY_FILTER_TYPE = "net.bicou.redmine.app.issues.IssueListFilterType";
	public static final String KEY_FILTER_ID = "net.bicou.redmine.app.issues.IssueListFilterId";
	public static final String KEY_FILTER_SEARCH_TERMS = "net.bicou.redmine.app.issues.IssueListFilterSearchTerms";

	public static final String KEY_HAS_FILTER = "net.bicou.redmine.app.issues.HasFilter";

	public enum FilterType {
		ALL,
		QUERY,
		PROJECT,
		VERSION,
		SEARCH,
	}

	;

	public IssuesListFilter.FilterType type;
	public long id;
	public long serverId;
	public String searchQuery;

	public static final IssuesListFilter FILTER_ALL = new IssuesListFilter(0, FilterType.ALL, 0);

	/**
	 * Creates a filter that is not a FilterType.SEARCH
	 *
	 * @param serverId
	 *            The Server ID
	 * @param filterType
	 *            The filter Type
	 * @param itemId
	 *            The filter row ID
	 */
	public IssuesListFilter(final long serverId, final FilterType filterType, final long itemId) {
		this.serverId = serverId;
		this.type = filterType;
		this.id = itemId;
	}

	/**
	 * Creates a FilterType.SEARCH filter
	 *
	 * @param search
	 *            Search terms
	 */
	public IssuesListFilter(final String search) {
		serverId = 0;
		type = FilterType.SEARCH;
		id = 0;
		searchQuery = search;
	}

	public IssuesListFilter(final Bundle args) {
		serverId = args.getLong(KEY_FILTER_SERVER_ID);
		type = FilterType.valueOf(args.getString(KEY_FILTER_TYPE));
		id = args.getLong(KEY_FILTER_ID);
		searchQuery = args.getString(KEY_FILTER_SEARCH_TERMS);
	}

	public void saveTo(final Bundle args) {
		args.putBoolean(KEY_HAS_FILTER, true);
		args.putLong(KEY_FILTER_SERVER_ID, serverId);
		args.putString(KEY_FILTER_TYPE, type.name());
		args.putLong(KEY_FILTER_ID, id);
		args.putString(KEY_FILTER_SEARCH_TERMS, searchQuery);
	}

	public static IssuesListFilter fromBundle(Bundle args) {
		if (args.getBoolean(KEY_HAS_FILTER)) {
			return new IssuesListFilter(args);
		}
		return FILTER_ALL;
	}
}
