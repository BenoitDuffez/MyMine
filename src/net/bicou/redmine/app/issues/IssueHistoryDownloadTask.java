package net.bicou.redmine.app.issues;

import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.*;
import net.bicou.redmine.data.sqlite.*;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.util.DiffMatchPatch;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class IssueHistoryDownloadTask extends AsyncTask<Void, Void, IssueHistory> {
	Issue mIssue;
	SherlockFragmentActivity mActivity;
	JournalsDownloadCallbacks mCallbacks;
	File mCacheFolder;

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

	public interface JournalsDownloadCallbacks {
		void onPreExecute();

		void onJournalsDownloaded(IssueHistory history);
	}

	public IssueHistoryDownloadTask(final SherlockFragmentActivity act, final JournalsDownloadCallbacks callbacks, final Issue issue) {
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

		parseJournals(history.journals);
		parseChangeSets(history.changesets);

		return history;
	}

	/**
	 * Download the issue and its journals
	 *
	 * @return
	 */
	private IssueHistory downloadHistory() {
		// Download issue JSON
		final String url = String.format(Locale.ENGLISH, "issues/%d.json", mIssue.id);
		final NameValuePair[] args = new BasicNameValuePair[] {
				new BasicNameValuePair("include", "journals,changesets"),
		};
		return new JsonDownloader<IssueHistory>(IssueHistory.class) //
				.setStripJsonContainer(true) //
				.fetchObject(mActivity, mIssue.server, url, args);
	}

	/**
	 * Translate a version change
	 *
	 * @param d
	 * @return
	 */
	private PropertyChange onVersionChanged(final JournalDetail d) {
		String propName, oldVal, newVal;

		propName = mActivity.getString(R.string.issue_target_version);
		int oldId = 0, newId = 0;
		try {
			if (!TextUtils.isEmpty(d.old_value)) {
				oldId = Integer.parseInt(d.old_value);
			}
			if (!TextUtils.isEmpty(d.new_value)) {
				newId = Integer.parseInt(d.new_value);
			}
		} catch (final NumberFormatException nfe) {
		}
		final VersionsDbAdapter db = new VersionsDbAdapter(mActivity);
		db.open();
		if (oldId <= 0) {
			oldVal = null;
		} else {
			oldVal = db.getName(mIssue.server, mIssue.project, oldId);
		}
		if (newId <= 0) {
			newVal = null;
		} else {
			newVal = db.getName(mIssue.server, mIssue.project, newId);
		}
		db.close();

		return new PropertyChange(propName, oldVal, newVal);
	}

	private PropertyChange onEstimatedHoursChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_estimated_hours), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onDoneRatioChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_percent_done), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onAssignedToChange(final JournalDetail d) {
		final String oldVal, newVal;
		int oldId = 0, newId = 0;
		try {
			if (!TextUtils.isEmpty(d.old_value)) {
				oldId = Integer.parseInt(d.old_value);
			}
			if (!TextUtils.isEmpty(d.new_value)) {
				newId = Integer.parseInt(d.new_value);
			}
		} catch (final NumberFormatException nfe) {
		}

		final UsersDbAdapter db = new UsersDbAdapter(mActivity);
		db.open();
		User o = null, n = null;
		if (oldId > 0) {
			o = db.select(mIssue.server, oldId);
		}
		if (newId > 0) {
			n = db.select(mIssue.server, newId);
		}
		db.close();

		oldVal = o == null ? mActivity.getString(R.string.issue_journal_value_na) : o.firstname + " " + o.lastname;
		newVal = n == null ? mActivity.getString(R.string.issue_journal_value_na) : n.firstname + " " + n.lastname;

		return new PropertyChange(mActivity.getString(R.string.issue_assignee), oldVal, newVal);
	}

	private PropertyChange onPriorityChange(final JournalDetail d) {
		String oldVal, newVal;
		int oldId = 0, newId = 0;
		try {
			if (!TextUtils.isEmpty(d.old_value)) {
				oldId = Integer.parseInt(d.old_value);
			}
			if (!TextUtils.isEmpty(d.new_value)) {
				newId = Integer.parseInt(d.new_value);
			}
		} catch (final NumberFormatException nfe) {
		}

		IssuePrioritiesDbAdapter db = new IssuePrioritiesDbAdapter(mActivity);
		db.open();
		IssuePriority o = null, n = null;
		if (oldId > 0) {
			o = db.select(mIssue.server, oldId, null);
		}
		if (newId > 0) {
			n = db.select(mIssue.server, newId, null);
		}

		oldVal = o == null ? mActivity.getString(R.string.issue_journal_value_na) : o.name;
		newVal = n == null ? mActivity.getString(R.string.issue_journal_value_na) : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_priority), oldVal, newVal);
	}

	private PropertyChange onStatusChange(final JournalDetail d) {
		String oldVal, newVal;

		final IssueStatusesDbAdapter db = new IssueStatusesDbAdapter(mActivity);
		db.open();
		try {
			final long oldStatus = Long.parseLong(d.old_value);
			oldVal = db.getName(mIssue.server, oldStatus);
		} catch (final Exception e) {
			oldVal = null;
		}
		try {
			final long newStatus = Long.parseLong(d.new_value);
			newVal = db.getName(mIssue.server, newStatus);
		} catch (final Exception e) {
			newVal = null;
		}
		db.close();

		return new PropertyChange(mActivity.getString(R.string.issue_status), oldVal, newVal);
	}

	private PropertyChange onDueDateChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_due_date), d.old_value, d.new_value); // TODO
	}

	private PropertyChange onSubjectChange(final JournalDetail d) {
		return new PropertyChange(mActivity.getString(R.string.issue_subject), d.old_value, d.new_value);
	}

	private PropertyChange onTrackerChange(JournalDetail d) {
		TrackersDbAdapter db = new TrackersDbAdapter(mActivity);
		db.open();

		int oldId = 0, newId = 0;
		try {
			oldId = Integer.parseInt(d.old_value);
			newId = Integer.parseInt(d.new_value);
		} catch (NumberFormatException e) {
		}
		Tracker o = db.select(mIssue.server, oldId);
		Tracker n = db.select(mIssue.server, newId);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_tracker), oldValue, newValue);
	}

	private PropertyChange onIssueCategoryChange(JournalDetail d) {
		IssueCategoriesDbAdapter db = new IssueCategoriesDbAdapter(mActivity);
		db.open();

		long oldId = 0, newId = 0;
		try {
			oldId = Long.parseLong(d.old_value);
			newId = Long.parseLong(d.new_value);
		} catch (NumberFormatException e) {
		}

		IssueCategory o = db.select(mIssue.server, mIssue.project, oldId, null);
		IssueCategory n = db.select(mIssue.server, mIssue.project, newId, null);

		String oldValue = o == null ? null : o.name;
		String newValue = n == null ? null : n.name;

		return new PropertyChange(mActivity.getString(R.string.issue_category), oldValue, newValue);
	}

	private void setAvatarUrl(final Journal journal) {
		final UsersDbAdapter db = new UsersDbAdapter(mActivity);
		db.open();
		journal.user = db.select(mIssue.server, journal.user.id);
		db.close();

		if (journal.user == null || TextUtils.isEmpty(journal.user.mail)) {
			return;
		}

		journal.user.createGravatarUrl();
	}

	private PropertyChange onDescriptionChange(JournalDetail d) {
		String oldHtml = Util.htmlFromTextile(d.old_value);
		String newHtml = Util.htmlFromTextile(d.new_value);
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

	private List<String> getFormattedDetails(final Journal journal) {
		final List<String> formattedDetails = new ArrayList<String>();
		PropertyChange propChange;

		for (final JournalDetail d : journal.details) {
			if ("attr".equals(d.property)) {
				// Get the human readable name of the property that was changed
				if (IssuesDbAdapter.KEY_FIXED_VERSION_ID.equals(d.name)) {
					propChange = onVersionChanged(d);
				} else if (IssuesDbAdapter.KEY_ESTIMATED_HOURS.equals(d.name)) {
					propChange = onEstimatedHoursChange(d);
				} else if (IssuesDbAdapter.KEY_DONE_RATIO.equals(d.name)) {
					propChange = onDoneRatioChange(d);
				} else if (IssuesDbAdapter.KEY_ASSIGNED_TO_ID.equals(d.name)) {
					propChange = onAssignedToChange(d);
				} else if (IssuesDbAdapter.KEY_PRIORITY_ID.equals(d.name)) {
					propChange = onPriorityChange(d);
				} else if (IssuesDbAdapter.KEY_STATUS_ID.equals(d.name)) {
					propChange = onStatusChange(d);
				} else if (IssuesDbAdapter.KEY_DUE_DATE.equals(d.name)) {
					propChange = onDueDateChange(d);
				} else if (IssuesDbAdapter.KEY_SUBJECT.equals(d.name)) {
					propChange = onSubjectChange(d);
				} else if (IssuesDbAdapter.KEY_TRACKER_ID.equals(d.name)) {
					propChange = onTrackerChange(d);
				} else if (IssuesDbAdapter.KEY_CATEGORY_ID.equals(d.name)) {
					propChange = onIssueCategoryChange(d);
				} else if (IssuesDbAdapter.KEY_DESCRIPTION.equals(d.name)) {
					propChange = onDescriptionChange(d);
				} else {
					propChange = new PropertyChange(mActivity.getString(R.string.issue_journal_unknown_property, d.name), d.old_value, d.new_value);
					L.e("Unknown property " + d.property + " name: " + d.name + " old=" + d.old_value + " new=" + d.new_value, null);
				}

				// Make a sentence of what happened to that property
				String propertyChange;
				if (!TextUtils.isEmpty(d.old_value) && !TextUtils.isEmpty(d.new_value)) {
					propertyChange = mActivity.getString(R.string.issue_journal_property_changed_fromto, propChange.propName, propChange.oldVal,
							propChange.newVal);
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
			} else {
				L.e("Unknown property " + d.property + " name: " + d.name + " old=" + d.old_value + " new=" + d.new_value, null);
			}
		}

		return formattedDetails;
	}

	private CharSequence trim(CharSequence s) {
		int start = 0, end = s.length();
		while (start < end && Character.isWhitespace(s.charAt(start))) {
			start++;
		}

		while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
			end--;
		}

		return s.subSequence(start, end);
	}

	/**
	 * Translate data in the journal into human-readable values
	 */
	private void parseJournals(final List<Journal> journals) {
		for (final Journal journal : journals) {
			// Avatar
			setAvatarUrl(journal);

			// Details
			journal.formatted_details = getFormattedDetails(journal);

			// Notes
			if (TextUtils.isEmpty(journal.notes) == false) {
				String html = Util.htmlFromTextile(journal.notes);
				html = html.replace("<pre>", "<tt>").replace("</pre>", "</tt>");
				// Fake lists
				html = html.replace("<li>", " &nbsp; &nbsp; • ").replace("</li>", "<br />");
				journal.formatted_notes = (Spanned) trim(Html.fromHtml(html));
			}
		}
	}

	private void parseChangeSets(List<ChangeSet> changeSets) {
		if (changeSets == null || changeSets.size() <= 0) {
			return;
		}

		UsersDbAdapter db = new UsersDbAdapter(mActivity);
		db.open();
		for (ChangeSet changeSet : changeSets) {
			changeSet.commentsHtml = Html.fromHtml(changeSet.comments.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br />"));
			// TODO: cache users?
			if (changeSet.user != null) {
				changeSet.user = db.select(mIssue.server, changeSet.user.id);
				changeSet.user.createGravatarUrl();
			}
		}
		db.close();
	}

	@Override
	protected void onPostExecute(final IssueHistory history) {
		if (mCallbacks != null) {
			mCallbacks.onJournalsDownloaded(history);
		}
	}

	@Override
	protected void onPreExecute() {
		if (mCallbacks != null) {
			mCallbacks.onPreExecute();
		}
	}
}
