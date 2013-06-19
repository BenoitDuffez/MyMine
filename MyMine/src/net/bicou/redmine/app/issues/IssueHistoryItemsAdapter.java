package net.bicou.redmine.app.issues;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.ChangeSet;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueHistory;
import net.bicou.redmine.data.json.Journal;
import net.bicou.redmine.util.StrikeTagHandler;

import java.util.Date;
import java.util.Locale;

class IssueHistoryItemsAdapter extends BaseAdapter {
	Issue mIssue;
	IssueHistory mHistory;
	Context mContext;
	java.text.DateFormat mDateFormat;
	java.text.DateFormat mTimeFormat;

	static class JournalViewsHolder {
		TextView user, date, details, notes;
		ImageView avatar;
	}

	public IssueHistoryItemsAdapter(final Context context, Issue issue, IssueHistory history) {
		mIssue = issue;
		mHistory = history;
		mContext = context;
		mDateFormat = DateFormat.getLongDateFormat(mContext);
		mTimeFormat = DateFormat.getTimeFormat(mContext);
	}

	public int getCount() {
		if (mHistory != null) {
			return mHistory.size();
		}
		return 0;
	}

	public Object getItem(int position) {
		if (mHistory != null) {
			return mHistory.getItem(position);
		}
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		final JournalViewsHolder holder;
		if (convertView == null) {
			final LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.issue_journal_list_item, null);

			holder = new JournalViewsHolder();
			holder.user = (TextView) convertView.findViewById(R.id.issue_journal_item_user);
			holder.date = (TextView) convertView.findViewById(R.id.issue_journal_item_date);
			holder.details = (TextView) convertView.findViewById(R.id.issue_journal_item_details);
			holder.notes = (TextView) convertView.findViewById(R.id.issue_journal_item_notes);
			holder.avatar = (ImageView) convertView.findViewById(R.id.issue_journal_item_avatar);

			convertView.setTag(holder);
		} else {
			holder = (JournalViewsHolder) convertView.getTag();
		}

		Object item = getItem(position);
		if (item != null) {
			if (item instanceof Journal) {
				Journal journal = (Journal) item;
				// User & date
				holder.user.setText(journal.user == null ? mContext.getString(R.string.issue_journal_user_anonymous) : journal.user.getName());
				final Date d = journal.created_on.getTime();
				holder.date.setText(String.format("%s — %s", mDateFormat.format(d), mTimeFormat.format(d)));

				// Show details
				final StringBuilder html = new StringBuilder();
				for (final String detail : journal.formatted_details) {
					html.append("&nbsp; • ").append(detail).append("<br />\n");
				}
				holder.details.setText(Html.fromHtml(html.toString(), null, new StrikeTagHandler()));
				holder.details.setMovementMethod(LinkMovementMethod.getInstance());

				// Show notes
				if (TextUtils.isEmpty(journal.notes)) {
					holder.notes.setVisibility(View.GONE);
				} else {
					holder.notes.setVisibility(View.VISIBLE);
					holder.notes.setText(journal.formatted_notes);
					Linkify.addLinks(holder.notes, Linkify.ALL & ~Linkify.PHONE_NUMBERS);
				}

				if (journal.user == null || TextUtils.isEmpty(journal.user.gravatarUrl)) {
					holder.avatar.setVisibility(View.INVISIBLE);
				} else {
					holder.avatar.setVisibility(View.VISIBLE);
					ImageLoader.getInstance().displayImage(journal.user.gravatarUrl, holder.avatar);
				}
			} else {
				ChangeSet changeSet = (ChangeSet) item;
				holder.user.setText(changeSet.user == null ? mContext.getString(R.string.issue_journal_user_anonymous) : changeSet.user.getName());
				Date d = changeSet.committed_on.getTime();
				holder.date.setText(String.format(Locale.ENGLISH, "%s — %s", mDateFormat.format(d), mTimeFormat.format(d)));
				holder.details.setText(mContext.getString(R.string.issue_changeset_revision, changeSet.revision));// TODO create link to revision
				holder.notes.setText(changeSet.commentsHtml);

				if (TextUtils.isEmpty(changeSet.user.gravatarUrl)) {
					holder.avatar.setVisibility(View.INVISIBLE);
				} else {
					holder.avatar.setVisibility(View.VISIBLE);
					ImageLoader.getInstance().displayImage(changeSet.user.gravatarUrl, holder.avatar);
				}
			}
		}

		return convertView;
	}
}
