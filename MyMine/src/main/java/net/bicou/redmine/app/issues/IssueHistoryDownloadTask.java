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

public class IssueHistoryDownloadTask extends AsyncTask<Void, Void, IssueHistory> {
	Issue mIssue;
	ActionBarActivity mActivity;
	JournalsDownloadCallbacks mCallbacks;
	File mCacheFolder;
	JsonNetworkError mError;

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

	private static class IdPair {
		public long oldId, newId;
		private static IdPair instance;

		private IdPair() {
		}

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

	public interface JournalsDownloadCallbacks {
		void onPreExecute();

		void onJournalsDownloaded(IssueHistory history);

		void onJournalsFailed(JsonNetworkError error);
	}

	public IssueHistoryDownloadTask(final ActionBarActivity act, final JournalsDownloadCallbacks callbacks, final Issue issue) {
		mActivity = act;
		mCallbacks = callbacks;
		mIssue = issue;
		mCacheFolder = act.getCacheDir();
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

		IssuesDbAdapter dummy = new IssuesDbAdapter(mActivity);
		dummy.open();
		parseJournals(history.journals, dummy);
		parseChangeSets(history.changesets, dummy);
		dummy.close();

		return history;
	}

	/**
	 * Download the issue and its journals
	 */
	private IssueHistory downloadHistory() {
		// Download issue JSON
		final String url = String.format(Locale.ENGLISH, "issues/%d.json", mIssue.id);
		final NameValuePair[] args = new BasicNameValuePair[] { new BasicNameValuePair("include", "journals,changesets"), };

		JsonDownloader<IssueHistory> downloader = new JsonDownloader<IssueHistory>(IssueHistory.class).setStripJsonContainer(true);
		IssueHistory history = downloader.fetchObject(mActivity, mIssue.server, url, args);
		if (history == null) {
			mError = downloader.getError();
		}

		return history;
	}

	/**
	 * Translate a version change
	 */
	private PropertyChange onVersionChanged(final JournalDetail d, IdPair ids, DbAdapter db) {
		String propName, oldVal, newVal;

		propName = mActivity.getString(R.string.issue_target_version);
		final VersionsDbAdapter vdb = new VersionsDbAdapter(db);
		oldVal = vdb.getName(mIssue.server, mIssue.project, ids.oldId);
		newVal = vdb.getName(mIssue.server, mIssue.project, ids.newId);

		return new PropertyChange(propName, oldVal, newVal);
	}

	private PropertyChange onEstimatedHoursChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		return new PropertyChange(mActivity.getString(R.string.issue_estimated_hours), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onDoneRatioChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		return new PropertyChange(mActivity.getString(R.string.issue_percent_done), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onAssignedToChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		final String oldVal, newVal;

		final UsersDbAdapter udb = new UsersDbAdapter(db);
		User o, n;
		o = udb.select(mIssue.server, ids.oldId);
		n = udb.select(mIssue.server, ids.newId);

		oldVal = o == null ? mActivity.getString(R.string.issue_journal_value_na) : o.firstname + " " + o.lastname;
		newVal = n == null ? mActivity.getString(R.string.issue_journal_value_na) : n.firstname + " " + n.lastname;

		return new PropertyChange(mActivity.getString(R.string.issue_assignee), oldVal, newVal);
	}

	private PropertyChange onPriorityChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		String oldVal, newVal;

		IssuePrioritiesDbAdapter ipdb = new IssuePrioritiesDbAdapter(db);
		IssuePriority o, n;
		o = ipdb.select(mIssue.server, ids.oldId, null);
		n = ipdb.select(mIssue.server, ids.newId, null);

		oldVal = o == null ? mActivity.getString(R.string.issue_journal_value_na) : o.name;
		newVal = n == null ? mActivity.getString(R.string.issue_journal_value_na) : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_priority), oldVal, newVal);
	}

	private PropertyChange onStatusChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		String oldVal, newVal;

		final IssueStatusesDbAdapter isdb = new IssueStatusesDbAdapter(db);
		oldVal = isdb.getName(mIssue.server, ids.oldId);
		newVal = isdb.getName(mIssue.server, ids.newId);

		return new PropertyChange(mActivity.getString(R.string.issue_status), oldVal, newVal);
	}

	private PropertyChange onDueDateChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		return new PropertyChange(mActivity.getString(R.string.issue_due_date), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onStartDateChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		return new PropertyChange(mActivity.getString(R.string.issue_start_date), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onSubjectChange(final JournalDetail d, IdPair ids, DbAdapter db) {
		return new PropertyChange(mActivity.getString(R.string.issue_subject), d.old_value, d.new_value);
	}

	private PropertyChange onTrackerChange(JournalDetail d, IdPair ids, DbAdapter db) {
		TrackersDbAdapter tdb = new TrackersDbAdapter(db);
		Tracker o = tdb.select(mIssue.server, ids.oldId);
		Tracker n = tdb.select(mIssue.server, ids.newId);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_tracker), oldValue, newValue);
	}

	private PropertyChange onIssueCategoryChange(JournalDetail d, IdPair ids, DbAdapter db) {
		IssueCategoriesDbAdapter icdb = new IssueCategoriesDbAdapter(db);

		IssueCategory o = icdb.select(mIssue.server, mIssue.project, ids.oldId, null);
		IssueCategory n = icdb.select(mIssue.server, mIssue.project, ids.newId, null);

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

	private PropertyChange onDescriptionChange(JournalDetail d, IdPair ids, DbAdapter db) {
		String oldHtml = WikiUtils.htmlFromTextile(d.old_value);
		String newHtml = WikiUtils.htmlFromTextile(d.new_value);
		DiffMatchPatch diff = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> diffs = diff.diff_main(oldHtml, newHtml, false);
		diff.diff_cleanupEfficiency(diffs);

		StringBuilder sb = new StringBuilder("<br /><cite>");
		for (DiffMatchPatch.Diff difference : diffs) {
			difference.text = difference.text.replace("\n", "<br />");
			switch (difference.operation) {
			case DELETE:
				sb.append("<s>").append(difference.text).append("</s>");
				break;
			case EQUAL:
				sb.append(difference.text);
				break;
			case INSERT:
				sb.append("<b>").append(difference.text).append("</b>");
				break;
			}
		}

		// Force remove previous description
		d.old_value = null;

		return new PropertyChange(mActivity.getString(R.string.issue_description), null, sb.toString());
	}

	private PropertyChange onIsPrivateChange(JournalDetail d, IdPair ids, DbAdapter db) {
		String o = mActivity.getString(ids.oldId > 0 ? R.string.yes : R.string.no);
		String n = mActivity.getString(ids.newId > 0 ? R.string.yes : R.string.no);
		return new PropertyChange(mActivity.getString(R.string.issue_is_private), o, n);
	}

	private PropertyChange onParentChange(JournalDetail d, IdPair ids, DbAdapter db) {
		String o = ids.oldId > 0 ? "#" + Long.toString(ids.oldId) : null;
		String n = ids.newId > 0 ? "#" + Long.toString(ids.newId) : null;
		return new PropertyChange(mActivity.getString(R.string.issue_parent), o, n);
	}

	private PropertyChange onProjectChange(JournalDetail d, IdPair ids, DbAdapter db) {
		ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);

		Project o = pdb.select(mIssue.server, ids.oldId, null);
		Project n = pdb.select(mIssue.server, ids.newId, null);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_project), oldValue, newValue);
	}

	private List<String> getFormattedDetails(final Journal journal, DbAdapter db) {
		final List<String> formattedDetails = new ArrayList<String>();
		PropertyChange propChange;
		IdPair ids;

		for (final JournalDetail d : journal.details) {
			ids = IdPair.from(d);
			if ("attr".equals(d.property) || "attribute".equals(d.property)) {
				// Get the human readable name of the property that was changed
				if (IssuesDbAdapter.KEY_FIXED_VERSION_ID.equals(d.name)) {
					propChange = onVersionChanged(d, ids, db);
				} else if (IssuesDbAdapter.KEY_ESTIMATED_HOURS.equals(d.name)) {
					propChange = onEstimatedHoursChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_DONE_RATIO.equals(d.name)) {
					propChange = onDoneRatioChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_ASSIGNED_TO_ID.equals(d.name)) {
					propChange = onAssignedToChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_PRIORITY_ID.equals(d.name)) {
					propChange = onPriorityChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_STATUS_ID.equals(d.name)) {
					propChange = onStatusChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_DUE_DATE.equals(d.name)) {
					propChange = onDueDateChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_START_DATE.equals(d.name)) {
					propChange = onStartDateChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_SUBJECT.equals(d.name)) {
					propChange = onSubjectChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_TRACKER_ID.equals(d.name)) {
					propChange = onTrackerChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_CATEGORY_ID.equals(d.name)) {
					propChange = onIssueCategoryChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_DESCRIPTION.equals(d.name)) {
					propChange = onDescriptionChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_IS_PRIVATE.equals(d.name)) {
					propChange = onIsPrivateChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_PARENT_ID.equals(d.name)) {
					propChange = onParentChange(d, ids, db);
				} else if (IssuesDbAdapter.KEY_PROJECT_ID.equals(d.name)) {
					propChange = onProjectChange(d, ids, db);
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

	@Override
	protected void onPreExecute() {
		if (mCallbacks != null) {
			mCallbacks.onPreExecute();
		}
	}
}
