package net.bicou.redmine.app.issues.issue;

import android.content.Context;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;

import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Journal;

import java.util.Date;
import java.util.List;

class IssueJournalsAdapter extends IssueItemsAdapter<Journal> {
	public IssueJournalsAdapter(Context context, List<Journal> attachments) {
		super(context, attachments);
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

	@Override
	protected void bindData(JournalViewsHolder holder, Journal journal) {
		// User & date
		holder.user.setText(journal.user == null ? mContext.getString(R.string.issue_journal_user_anonymous) : journal.user.getName());
		final Date d = journal.created_on.getTime();
		holder.date.setText(String.format("%s — %s", mDateFormat.format(d), mTimeFormat.format(d)));

		// Show details
		holder.details.setText(journal.formatted_details);
		holder.details.setMovementMethod(LinkMovementMethod.getInstance());

		// Show notes
		if (TextUtils.isEmpty(journal.notes)) {
			holder.notes.setVisibility(View.GONE);
		} else {
			holder.notes.setVisibility(View.VISIBLE);
			holder.notes.setText(trim(journal.formatted_notes));
			Linkify.addLinks(holder.notes, Linkify.ALL & ~Linkify.PHONE_NUMBERS);
		}

		if (journal.user == null || TextUtils.isEmpty(journal.user.gravatarUrl)) {
			holder.avatar.setVisibility(View.INVISIBLE);
		} else {
			holder.avatar.setVisibility(View.VISIBLE);
			ImageLoader.getInstance().displayImage(journal.user.gravatarUrl, holder.avatar);
		}
	}
}
