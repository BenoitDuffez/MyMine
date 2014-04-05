package net.bicou.redmine.app.issues;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;

import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.JournalDetail;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.net.JsonNetworkError;

import java.io.File;

/**
 * Wrapper around an {@link android.os.AsyncTask} that downloads an issue history (journal or revisions), and parses it so that it is ready to be displayed on the UI thread.
 */
public abstract class IssueHistoryDownloadTask<Type> extends AsyncTask<Void, Void, Type> {
	Issue mIssue;
	ActionBarActivity mActivity;
	File mCacheFolder;
	JsonNetworkError mError;

	/**
	 * Helper class containing human-readable property change information (property name, old and new values)
	 */
	protected static class PropertyChange {
		public String propName;
		public String oldVal;
		public String newVal;

		public PropertyChange(final String name, final String o, final String n) {
			propName = name;
			oldVal = o;
			newVal = n;
		}
	}

	/**
	 * Helper class that will parse the {@link net.bicou.redmine.data.json.JournalDetail} provided in order to retrieve the previous/new ID of the
	 * object/property that was edited by someone. The IDs are parsed into {@code long}.
	 */
	protected static class IdPair {
		public long oldId, newId;
		private static IdPair instance;

		/**
		 * Prevent object construction
		 */
		private IdPair() {
		}

		/**
		 * Translates the property IDs to {@code long}
		 *
		 * @param d the property change information
		 *
		 * @return a new {@link net.bicou.redmine.app.issues.IssueHistoryDownloadTask.IdPair} object, containing the previous and new IDs as long.
		 */
		public static IdPair from(JournalDetail d) {
			if (instance == null) {
				instance = new IdPair();
			}
			instance.oldId = instance.newId = 0;
			if (d == null) {
				return instance;
			}
			if (!TextUtils.isEmpty(d.old_value)) {
				try {
					instance.oldId = Long.parseLong(d.old_value);
				} catch (Exception e) {
					instance.oldId = 0;
				}
			}
			if (!TextUtils.isEmpty(d.new_value)) {
				try {
					instance.newId = Long.parseLong(d.new_value);
				} catch (Exception e) {
					instance.newId = 0;
				}
			}

			return instance;
		}
	}

	/**
	 * Default constructor
	 *
	 * @param act   Used for database opening (as a {@link android.content.Context}) and notifications holder (for {@link de.keyboardsurfer.android.widget.crouton.Crouton})
	 * @param issue The issue object for which we need to download/parse stuff
	 */
	public IssueHistoryDownloadTask(final ActionBarActivity act, final Issue issue) {
		mActivity = act;
		mIssue = issue;
		mCacheFolder = act.getCacheDir();
	}

	/**
	 * Called before the background processing has started
	 */
	@Override
	protected abstract void onPreExecute();

	/**
	 * Called during the background task processing. Download and parse the data that needs to be displayed in the fragment.
	 *
	 * @param db A {@link net.bicou.redmine.data.sqlite.DbAdapter} that is kept open
	 *
	 * @return The downloaded/parsed data
	 */
	protected abstract Type downloadAndParse(DbAdapter db);

	@Override
	public final Type doInBackground(final Void... params) {
		if (mIssue == null || mIssue.server == null || mIssue.id <= 0) {
			return null;
		}

		// Dummy DB connection that we will keep open during all the background task, in order to avoid opening/closing the DB connection all the time.
		IssuesDbAdapter dummy = new IssuesDbAdapter(mActivity);
		dummy.open();
		final Type history = downloadAndParse(dummy);
		dummy.close();

		return history;
	}

	/**
	 * Called when the background processing is done
	 *
	 * @param history The downloaded/parsed data
	 */
	@Override
	protected abstract void onPostExecute(final Type history);
}
