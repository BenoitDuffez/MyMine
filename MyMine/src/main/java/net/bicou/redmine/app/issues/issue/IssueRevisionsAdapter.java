package net.bicou.redmine.app.issues.issue;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;

import net.bicou.redmine.R;
import net.bicou.redmine.data.json.ChangeSet;

import java.util.Date;
import java.util.List;
import java.util.Locale;

class IssueRevisionsAdapter extends IssueItemsAdapter<ChangeSet> {
	public IssueRevisionsAdapter(Context context, List<ChangeSet> attachments) {
		super(context, attachments);
	}

	@Override
	protected void bindData(IssueItemsAdapter.JournalViewsHolder holder, ChangeSet changeSet) {
		holder.user.setText(changeSet.user == null ? mContext.getString(R.string.issue_journal_user_anonymous) : changeSet.user.getName());
		Date d = changeSet.committed_on.getTime();
		holder.date.setText(String.format(Locale.ENGLISH, "%s — %s", mDateFormat.format(d), mTimeFormat.format(d)));
		holder.details.setText(mContext.getString(R.string.issue_changeset_revision, changeSet.revision));// TODO create link to revision
		holder.notes.setText(changeSet.commentsHtml);

		if (changeSet.user == null || TextUtils.isEmpty(changeSet.user.gravatarUrl)) {
			holder.avatar.setVisibility(View.INVISIBLE);
		} else {
			holder.avatar.setVisibility(View.VISIBLE);
			ImageLoader.getInstance().displayImage(changeSet.user.gravatarUrl, holder.avatar);
		}
	}
}
