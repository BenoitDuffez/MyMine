package net.bicou.redmine.app.issues;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import net.bicou.redmine.R;
import net.bicou.redmine.app.wiki.WikiUtils;
import net.bicou.redmine.data.json.ChangeSet;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueCategory;
import net.bicou.redmine.data.json.IssueHistory;
import net.bicou.redmine.data.json.IssuePriority;
import net.bicou.redmine.data.json.Journal;
import net.bicou.redmine.data.json.JournalDetail;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Tracker;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuePrioritiesDbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.TrackersDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.util.DiffMatchPatch;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.StrikeTagHandler;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper around an {@link android.os.AsyncTask} that downloads an issue history, and parses it so that it is ready to be displayed on the UI
 * thread.
 */
public class IssueHistoryDownloadTask extends AsyncTask<Void, Void, IssueHistory> {
	Issue mIssue;
	ActionBarActivity mActivity;
	JournalsDownloadCallbacks mCallbacks;
	File mCacheFolder;
	JsonNetworkError mError;

	/**
	 * Helper class containing human-readable property change information (property name, old and new values)
	 */
	private static class PropertyChange {
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
	private static class IdPair {
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
	 * {@link android.os.AsyncTask}-like methods called during the different steps of the background task
	 */
	public interface JournalsDownloadCallbacks {
		/**
		 * Executed on the same thread, before doing anything
		 */
		void onPreExecute();

		/**
		 * Executed on the same thread, after the issue history has been downloaded successfully
		 *
		 * @param history The downloaded and parsed issue history
		 */
		void onJournalsDownloaded(IssueHistory history);

		/**
		 * Executed on the same thread, after a failure in the download or the parsing of the issue history
		 *
		 * @param error An error describing what went wrong
		 */
		void onJournalsFailed(JsonNetworkError error);
	}

	public IssueHistoryDownloadTask(final ActionBarActivity act, final JournalsDownloadCallbacks callbacks, final Issue issue) {
		mActivity = act;
		mCallbacks = callbacks;
		mIssue = issue;
		mCacheFolder = act.getCacheDir();
	}

	@Override
	protected void onPreExecute() {
		if (mCallbacks != null) {
			mCallbacks.onPreExecute();
		}
	}

	@Override
	public IssueHistory doInBackground(final Void... params) {
		if (mIssue == null || mIssue.server == null || mIssue.id <= 0) {
			return null;
		}

		final IssueHistory history = downloadHistory();
		if (history == null || history.journals == null) {
			return null;
		}

		// Dummy DB connection that we will keep open during all the background task, in order to avoid opening/closing the DB connection all the
		// time.
		IssuesDbAdapter dummy = new IssuesDbAdapter(mActivity);
		dummy.open();
		parseJournals(history.journals, dummy);
		parseChangeSets(history.changesets, dummy);
		dummy.close();

		return history;
	}

	@Override
	protected void onPostExecute(final IssueHistory history) {
		if (mCallbacks != null) {
			if (history == null || history.journals == null) {
				mCallbacks.onJournalsFailed(mError);
			} else {
				mCallbacks.onJournalsDownloaded(history);
			}
		}
	}

	/**
	 * Download the issue and its journals
	 */
	private IssueHistory downloadHistory() {
		// Download issue JSON
		final String url = String.format(Locale.ENGLISH, "issues/%d.json", mIssue.id);
		final NameValuePair[] args = new BasicNameValuePair[] { new BasicNameValuePair("include", "journals,changesets"), };

		JsonDownloader<IssueHistory> downloader = new JsonDownloader<IssueHistory>(IssueHistory.class).stripJsonContainer(true);
		IssueHistory history = downloader.fetchObject(mActivity, mIssue.server, url, args);
		if (history == null) {
			mError = downloader.getError();
		}

		return history;
	}

	/**
	 * Translate data in the journal into human-readable values
	 */
	private void parseJournals(final List<Journal> journals, DbAdapter db) {
		for (final Journal journal : journals) {
			// Avatar
			setAvatarUrl(journal, db);

			// Details
			final StringBuilder details = new StringBuilder();
			for (final String detail : getFormattedDetails(journal, db)) {
				details.append("&nbsp; • ").append(detail).append("<br />\n");
			}
			journal.formatted_details = (SpannableStringBuilder) Html.fromHtml(details.toString(), null, new StrikeTagHandler());

			// Notes
			if (!TextUtils.isEmpty(journal.notes)) {
				String notes = WikiUtils.htmlFromTextile(journal.notes);
				notes = notes.replace("<pre>", "<tt>").replace("</pre>", "</tt>");
				// Fake lists
				notes = notes.replace("<li>", " &nbsp; &nbsp; • ").replace("</li>", "<br />");
				journal.formatted_notes = (SpannableStringBuilder) Html.fromHtml(notes);
			}
		}
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

	/**
	 * Will go through all the changes made in a single journal entry to get human-readable information
	 *
	 * @param journal The downloaded journal entry
	 * @param db      A {@link net.bicou.redmine.data.sqlite.DbAdapter} used to keep the same DB connection
	 * @return The list of changes as simple string sentences
	 */
	private List<String> getFormattedDetails(final Journal journal, DbAdapter db) {
		final List<String> formattedDetails = new ArrayList<String>();
		PropertyChange propChange;
		IdPair ids;

		for (final JournalDetail d : journal.details) {
			ids = IdPair.from(d);
			if ("attr".equals(d.property) || "attribute".equals(d.property)) {
				// Get the human readable name of the property that was changed
				if (IssuesDbAdapter.KEY_FIXED_VERSION_ID.equals(d.name)) {
					propChange = onVersionChanged(ids, db);
				} else if (IssuesDbAdapter.KEY_ESTIMATED_HOURS.equals(d.name)) {
					propChange = onEstimatedHoursChange(d);
				} else if (IssuesDbAdapter.KEY_DONE_RATIO.equals(d.name)) {
					propChange = onDoneRatioChange(d);
				} else if (IssuesDbAdapter.KEY_ASSIGNED_TO_ID.equals(d.name)) {
					propChange = onAssignedToChange(ids, db);
				} else if (IssuesDbAdapter.KEY_PRIORITY_ID.equals(d.name)) {
					propChange = onPriorityChange(ids, db);
				} else if (IssuesDbAdapter.KEY_STATUS_ID.equals(d.name)) {
					propChange = onStatusChange(ids, db);
				} else if (IssuesDbAdapter.KEY_DUE_DATE.equals(d.name)) {
					propChange = onDueDateChange(d);
				} else if (IssuesDbAdapter.KEY_START_DATE.equals(d.name)) {
					propChange = onStartDateChange(d);
				} else if (IssuesDbAdapter.KEY_SUBJECT.equals(d.name)) {
					propChange = onSubjectChange(d);
				} else if (IssuesDbAdapter.KEY_TRACKER_ID.equals(d.name)) {
					propChange = onTrackerChange(ids, db);
				} else if (IssuesDbAdapter.KEY_CATEGORY_ID.equals(d.name)) {
					propChange = onIssueCategoryChange(ids, db);
				} else if (IssuesDbAdapter.KEY_DESCRIPTION.equals(d.name)) {
					propChange = onDescriptionChange(d);
				} else if (IssuesDbAdapter.KEY_IS_PRIVATE.equals(d.name)) {
					propChange = onIsPrivateChange(ids);
				} else if (IssuesDbAdapter.KEY_PARENT_ID.equals(d.name)) {
					propChange = onParentChange(ids);
				} else if (IssuesDbAdapter.KEY_PROJECT_ID.equals(d.name)) {
					propChange = onProjectChange(ids, db);
				} else {
					propChange = new PropertyChange(mActivity.getString(R.string.issue_journal_unknown_property, d.name), d.old_value, d.new_value);
					L.e("Unknown property " + d.property + " name: " + d.name + " old=" + d.old_value + " new=" + d.new_value, null);
				}

				// Make a sentence of what happened to that property
				String propertyChange;
				if (!TextUtils.isEmpty(d.old_value) && !TextUtils.isEmpty(d.new_value)) {
					propertyChange = mActivity.getString(R.string.issue_journal_property_changed_fromto, propChange.propName, propChange.oldVal, propChange.newVal);
				} else if (!TextUtils.isEmpty(d.old_value)) {
					propertyChange = mActivity.getString(R.string.issue_journal_property_deleted, propChange.propName, propChange.oldVal);
				} else {
					propertyChange = mActivity.getString(R.string.issue_journal_property_changed_to, propChange.propName, propChange.newVal);
				}

				formattedDetails.add(propertyChange);
			} else if ("attachment".equals(d.property)) {
				String attnUrl = mIssue.server.serverUrl;
				if (!attnUrl.endsWith("/")) {
					attnUrl += "/";
				}
				attnUrl += "attachments/download/" + d.name + "/" + d.new_value;
				formattedDetails.add(mActivity.getString(R.string.issue_journal_attachment_added, attnUrl, d.new_value));
			} else if ("cf".equals(d.property)) {
				// TODO: support custom fields
				L.i("Custom fields are not yet supported. You can ask for support by emailing redmine@bicou.net");
			} else {
				L.e("Unknown journal detail " + d.property + " change, name: " + d.name + " old=" + d.old_value + " new=" + d.new_value, null);
			}
		}

		return formattedDetails;
	}

	/**
	 * Translate a version change
	 */
	private PropertyChange onVersionChanged(IdPair ids, DbAdapter db) {
		String propName, oldVal, newVal;

		propName = mActivity.getString(R.string.issue_target_version);
		final VersionsDbAdapter vdb = new VersionsDbAdapter(db);
		oldVal = vdb.getName(mIssue.server, mIssue.project, ids.oldId);
		newVal = vdb.getName(mIssue.server, mIssue.project, ids.newId);

		return new PropertyChange(propName, oldVal, newVal);
	}

	private PropertyChange onEstimatedHoursChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_estimated_hours), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onDoneRatioChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_percent_done), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onAssignedToChange(IdPair ids, DbAdapter db) {
		final String oldVal, newVal;

		final UsersDbAdapter udb = new UsersDbAdapter(db);
		User o, n;
		o = udb.select(mIssue.server, ids.oldId);
		n = udb.select(mIssue.server, ids.newId);

		oldVal = o == null ? mActivity.getString(R.string.issue_journal_value_na) : o.firstname + " " + o.lastname;
		newVal = n == null ? mActivity.getString(R.string.issue_journal_value_na) : n.firstname + " " + n.lastname;

		return new PropertyChange(mActivity.getString(R.string.issue_assignee), oldVal, newVal);
	}

	private PropertyChange onPriorityChange(IdPair ids, DbAdapter db) {
		String oldVal, newVal;

		IssuePrioritiesDbAdapter issuePrioritiesDb = new IssuePrioritiesDbAdapter(db);
		IssuePriority o, n;
		o = issuePrioritiesDb.select(mIssue.server, ids.oldId, null);
		n = issuePrioritiesDb.select(mIssue.server, ids.newId, null);

		oldVal = o == null ? mActivity.getString(R.string.issue_journal_value_na) : o.name;
		newVal = n == null ? mActivity.getString(R.string.issue_journal_value_na) : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_priority), oldVal, newVal);
	}

	private PropertyChange onStatusChange(IdPair ids, DbAdapter db) {
		String oldVal, newVal;

		final IssueStatusesDbAdapter issueStatusesDb = new IssueStatusesDbAdapter(db);
		oldVal = issueStatusesDb.getName(mIssue.server, ids.oldId);
		newVal = issueStatusesDb.getName(mIssue.server, ids.newId);

		return new PropertyChange(mActivity.getString(R.string.issue_status), oldVal, newVal);
	}

	private PropertyChange onDueDateChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_due_date), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onStartDateChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_start_date), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onSubjectChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_subject), d.old_value, d.new_value);
	}

	private PropertyChange onTrackerChange(IdPair ids, DbAdapter db) {
		TrackersDbAdapter tdb = new TrackersDbAdapter(db);
		Tracker o = tdb.select(mIssue.server, ids.oldId);
		Tracker n = tdb.select(mIssue.server, ids.newId);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_tracker), oldValue, newValue);
	}

	private PropertyChange onIssueCategoryChange(IdPair ids, DbAdapter db) {
		IssueCategoriesDbAdapter issueCategoriesDb = new IssueCategoriesDbAdapter(db);

		IssueCategory o = issueCategoriesDb.select(mIssue.server, mIssue.project, ids.oldId, null);
		IssueCategory n = issueCategoriesDb.select(mIssue.server, mIssue.project, ids.newId, null);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_category), oldValue, newValue);
	}

	private void setAvatarUrl(final Journal journal, DbAdapter db) {
		final UsersDbAdapter udb = new UsersDbAdapter(db);
		journal.user = udb.select(mIssue.server, journal.user.id);

		if (journal.user == null || TextUtils.isEmpty(journal.user.mail)) {
			return;
		}

		journal.user.createGravatarUrl();
	}

	private PropertyChange onDescriptionChange(JournalDetail d) {
		DiffMatchPatch diff = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> diffs = diff.diff_main(d.old_value, d.new_value, false);
		diff.diff_cleanupEfficiency(diffs);

		StringBuilder sb = new StringBuilder("\n");
		for (DiffMatchPatch.Diff difference : diffs) {
//			difference.text = difference.text.replace("\n", "<br />");
			switch (difference.operation) {
			case DELETE:
				sb.append("<font color=\"#990000\">").append(difference.text).append("</font>");
				break;
			case EQUAL:
				sb.append(difference.text);
				break;
			case INSERT:
				sb.append("<font color=\"#009900\">").append(difference.text).append("</font>");
				break;
			}
		}

		// Force remove previous description
		d.old_value = null;

		String newValue = sb.toString();
		newValue = newValue.replace("\n", "<br />");
		return new PropertyChange(mActivity.getString(R.string.issue_description), null, newValue);
	}

	private PropertyChange onIsPrivateChange(IdPair ids) {
		String o = mActivity.getString(ids.oldId > 0 ? R.string.yes : R.string.no);
		String n = mActivity.getString(ids.newId > 0 ? R.string.yes : R.string.no);
		return new PropertyChange(mActivity.getString(R.string.issue_is_private), o, n);
	}

	private PropertyChange onParentChange(IdPair ids) {
		String o = ids.oldId > 0 ? "#" + Long.toString(ids.oldId) : null;
		String n = ids.newId > 0 ? "#" + Long.toString(ids.newId) : null;
		return new PropertyChange(mActivity.getString(R.string.issue_parent), o, n);
	}

	private PropertyChange onProjectChange(IdPair ids, DbAdapter db) {
		ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);

		Project o = pdb.select(mIssue.server, ids.oldId, null);
		Project n = pdb.select(mIssue.server, ids.newId, null);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_project), oldValue, newValue);
	}
}
