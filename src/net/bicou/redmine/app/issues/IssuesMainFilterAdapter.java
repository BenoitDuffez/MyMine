package net.bicou.redmine.app.issues;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import net.bicou.redmine.R;
import net.bicou.redmine.app.SeparatorSpinnerAdapter;
import net.bicou.redmine.app.issues.IssuesListFilter.FilterType;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Query;
import android.content.Context;

public class IssuesMainFilterAdapter extends SeparatorSpinnerAdapter {
	public IssuesMainFilterAdapter(final Context ctx, final List<Query> queries, final List<Project> projects) {
		super(ctx, buildDataArray(queries, projects), R.layout.issues_filter_spinner_separator, R.layout.issues_filter_spinner_in_actionbar,
				R.layout.issues_filter_spinner, R.id.issues_filter_spinner_text);
	}

	private static List<Object> buildDataArray(final List<Query> queries, final List<Project> projects) {
		final List<Object> data = new ArrayList<Object>((queries == null ? 0 : queries.size()) + (projects == null ? 0 : projects.size()));
		data.add(null); // "ALL"
		if (queries != null && queries.size() > 0) {
			data.add(new SpinnerSeparator(R.string.issue_filter_queries));
			data.addAll(queries);
		}
		data.add(new SpinnerSeparator(R.string.issue_filter_projects));
		data.addAll(projects);
		return data;
	}

	public IssuesListFilter getFilter(final int position) {
		final Object item = getItem(position);
		if (item == null) {
			return IssuesListFilter.FILTER_ALL;
		} else if (item instanceof Query) {
			final Query q = (Query) item;
			return new IssuesListFilter(q.server.rowId, FilterType.QUERY, q.id);
		} else {
			final Project p = (Project) item;
			return new IssuesListFilter(p.server.rowId, FilterType.PROJECT, p.id);
		}
	}

	@Override
	public String getText(final int position) {
		final Object item = getItem(position);
		if (item == null) {
			return getString(R.string.issue_filter_all);
		} else if (item instanceof Query) {
			return ((Query) item).name;
		} else if (item instanceof Project) {
			return ((Project) item).name;
		} else if (item instanceof SpinnerSeparator) {
			return getString(((SpinnerSeparator) item).separatorTextResId);
		}
		throw new InvalidParameterException("Item has unknown type: " + item);
	}
}
