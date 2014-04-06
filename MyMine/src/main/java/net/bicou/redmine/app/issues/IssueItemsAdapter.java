package net.bicou.redmine.app.issues;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Attachment;
import net.bicou.redmine.util.Util;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by bicou on 06/04/2014.
 */
class IssueItemsAdapter extends BaseAdapter {
	private final List<Attachment> mAttachments;
	Context mContext;
	java.text.DateFormat mDateFormat;
	java.text.DateFormat mTimeFormat;

	static class JournalViewsHolder {
		TextView user, date, details, notes;
		ImageView avatar;
	}

	public IssueItemsAdapter(final Context context, List<Attachment> attachments) {
		mAttachments = attachments;
		mContext = context;
		if (mContext != null) {
			mDateFormat = DateFormat.getLongDateFormat(mContext);
			mTimeFormat = DateFormat.getTimeFormat(mContext);
		}
	}

	public int getCount() {
		if (mAttachments != null) {
			return mAttachments.size();
		}
		return 0;
	}

	public Attachment getItem(int position) {
		if (mAttachments != null) {
			return mAttachments.get(position);
		}
		return null;
	}

	public long getItemId(int position) {
		Attachment attachment = mAttachments.get(position);
		return attachment == null ? 0 : attachment.id;
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		if (mContext == null) {
			return null;
		}

		final JournalViewsHolder holder;
		if (convertView == null) {
			final LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.issue_journal_list_item, parent, false);

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

		Attachment attachment = getItem(position);
		if (attachment != null) {
			holder.user.setText(attachment.author == null ? mContext.getString(R.string.issue_journal_user_anonymous) : attachment.author.getName());
			if (!Util.isEpoch(attachment.created_on)) {
				Date d = attachment.created_on.getTime();
				holder.date.setText(String.format(Locale.ENGLISH, "%s — %s", mDateFormat.format(d), mTimeFormat.format(d)));
				holder.date.setVisibility(View.VISIBLE);
			} else {
				holder.date.setVisibility(View.INVISIBLE);
			}
			holder.details.setText(mContext.getString(R.string.issue_attachment_filename_size, attachment.filename, Util.readableFileSize(attachment.filesize)));
			holder.notes.setText(attachment.description);

			if (attachment.author == null || TextUtils.isEmpty(attachment.author.gravatarUrl)) {
				holder.avatar.setVisibility(View.INVISIBLE);
			} else {
				holder.avatar.setVisibility(View.VISIBLE);
				ImageLoader.getInstance().displayImage(attachment.author.gravatarUrl, holder.avatar);
			}
		}

		return convertView;
	}
}
