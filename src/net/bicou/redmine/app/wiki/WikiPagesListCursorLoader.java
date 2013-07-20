package net.bicou.redmine.app.wiki;

import android.content.Context;
import android.database.Cursor;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.SimpleCursorLoader;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;

/**
 * Loader that will handle the initial DB query and Cursor creation
 *
 * @author bicou
 */
public final class WikiPagesListCursorLoader extends SimpleCursorLoader {
	private final WikiDbAdapter mHelper;
	Project mProject;

	public WikiPagesListCursorLoader(final Context context, final WikiDbAdapter helper, Project project) {
		super(context);
		mHelper = helper;
		mProject = project;
	}

	@Override
	public Cursor loadInBackground() {
		return mHelper.selectAllCursor(mProject.server, mProject, new String[] {
				WikiDbAdapter.KEY_ROWID,
				WikiDbAdapter.KEY_TITLE,
				WikiDbAdapter.KEY_UPDATED_ON,
				WikiDbAdapter.KEY_SERVER_ID,
				WikiDbAdapter.KEY_PROJECT_ID,
		});
	}
}
