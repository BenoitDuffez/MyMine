package net.bicou.redmine.app.issues.issue;

import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;

import net.bicou.redmine.data.json.ChangeSet;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueRevisions;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.net.JsonNetworkError;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;
import java.util.Locale;

/**
 * Wrapper around an {@link android.os.AsyncTask} that downloads an issue history, and parses it so that it is ready to be displayed on the UI
 * thread.
 */
public class IssueRevisionsDownloadTask extends IssueHistoryDownloadTask<IssueRevisions> {
	RevisionsDownloadCallbacks mCallbacks;

	/**
	 * {@link android.os.AsyncTask}-like methods called during the different steps of the background task
	 */
	public interface RevisionsDownloadCallbacks {
		/**
		 * Executed on the same thread, before doing anything
		 */
		void onPreExecute();

		/**
		 * Executed on the same thread, after the issue history has been downloaded successfully
		 *
		 * @param history The downloaded and parsed issue history
		 */
		void onRevisionsDownloaded(IssueRevisions history);

		/**
		 * Executed on the same thread, after a failure in the download or the parsing of the issue history
		 *
		 * @param error An error describing what went wrong
		 */
		void onRevisionsDownloadFailed(JsonNetworkError error);
	}

	public IssueRevisionsDownloadTask(final ActionBarActivity act, final RevisionsDownloadCallbacks callbacks, final Issue issue) {
		super(act, issue);
		mCallbacks = callbacks;
	}

	@Override
	protected void onPreExecute() {
		if (mCallbacks != null) {
			mCallbacks.onPreExecute();
		}
	}

	@Override
	protected IssueRevisions downloadAndParse(DbAdapter db) {
		final IssueRevisions history = downloadRevisions();
		if (history == null || history.changesets == null) {
			return null;
		}
		parseChangeSets(history.changesets, db);
		return history;
	}

	@Override
	protected void onPostExecute(final IssueRevisions history) {
		if (mCallbacks != null) {
			if (history == null || history.changesets == null) {
				mCallbacks.onRevisionsDownloadFailed(mError);
			} else {
				mCallbacks.onRevisionsDownloaded(history);
			}
		}
	}

	/**
	 * Download the issue and its journals
	 */
	private IssueRevisions downloadRevisions() {
		// Download issue JSON
		final String url = String.format(Locale.ENGLISH, "issues/%d.json", mIssue.id);
		final NameValuePair[] args = new BasicNameValuePair[] { new BasicNameValuePair("include", "journals,changesets"), };

		JsonDownloader<IssueRevisions> downloader = new JsonDownloader<IssueRevisions>(IssueRevisions.class);
		downloader.setStripJsonContainer(true);
		IssueRevisions history = downloader.fetchObject(mActivity, mIssue.server, url, args);
		if (history == null) {
			mError = downloader.getError();
		}

		return history;
	}

	/**
	 * Translate data in the changesets (code revisions) into human-readable values
	 *
	 * @param changeSets The list of code revisions
	 * @param db         A A {@link net.bicou.redmine.data.sqlite.DbAdapter} used to keep the same DB connection
	 */
	private void parseChangeSets(List<ChangeSet> changeSets, DbAdapter db) {
		if (changeSets == null || changeSets.size() <= 0) {
			return;
		}

		UsersDbAdapter udb = new UsersDbAdapter(db);
		for (ChangeSet changeSet : changeSets) {
			changeSet.commentsHtml = (SpannableStringBuilder) Html.fromHtml(changeSet.comments.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br />"));
			// TODO: cache users?
			if (changeSet.user != null) {
				changeSet.user = udb.select(mIssue.server, changeSet.user.id);
				if (changeSet.user != null) {
					changeSet.user.createGravatarUrl();
				}
			}
		}
	}
}
