package net.bicou.redmine.app.issues;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import net.bicou.redmine.R;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.widget.IssuesListRelativeLayout;
import net.bicou.redmine.widget.NoParentPressImageView;

/**
 * CursorAdapter that will map Cursor data to a layout
 *
 * @author bicou
 */
public final class IssuesListCursorAdapter extends CursorAdapter {
	private final Context mContext;
	private IssueFavoriteToggleListener mListener;
	private float mDensity;
	int[] backgrounds;
	int[] textColors;
	int numColors;

	public class ViewHolder {
		IssuesListRelativeLayout layout;
		TextView issueID, subject, targetVersion, status, description, project;
		NoParentPressImageView favorite;
	}

	public interface IssueFavoriteToggleListener {
		public void onIssueFavoriteChanged(long serverId, long issueId, boolean isFavorite);
	}

	public IssuesListCursorAdapter(final Context context, final Cursor c, final int flags) {
		super(context, c, flags);
		mContext = context;
	}

	public IssuesListCursorAdapter(final Context context, final Cursor c, final boolean autoRequery, IssueFavoriteToggleListener listener) {
		super(context, c, autoRequery);
		mContext = context;
		Resources res = context.getResources();
		mDensity = res.getDisplayMetrics().density;
		textColors = res.getIntArray(R.array.issue_listitem_project_textcolors);
		TypedArray backgroundsIds = res.obtainTypedArray(R.array.issue_listitem_project_backgrounds);
		numColors = res.getInteger(R.integer.issue_listitem_project_num_colors);
		backgrounds = new int[numColors];
		for (int i = 0; i < numColors && backgroundsIds != null; i++) {
			backgrounds[i] = backgroundsIds.getResourceId(i, -1);
		}
		if (backgroundsIds != null) {
			backgroundsIds.recycle();
		}
		mListener = listener;
	}

	@Override
	public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
		final IssuesListRelativeLayout view = (IssuesListRelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.issue_listitem, parent, false);
		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.layout = view;
		viewHolder.issueID = (TextView) view.findViewById(R.id.issue_item_id);
		viewHolder.subject = (TextView) view.findViewById(R.id.issue_item_subject);
		viewHolder.targetVersion = (TextView) view.findViewById(R.id.issue_item_fixed_version);
		viewHolder.status = (TextView) view.findViewById(R.id.issue_item_status);
		viewHolder.description = (TextView) view.findViewById(R.id.issue_item_description);
		viewHolder.project = (TextView) view.findViewById(R.id.issue_item_project_name);
		viewHolder.favorite = (NoParentPressImageView) view.findViewById(R.id.issue_item_favorite);
		view.setTag(viewHolder);
		return view;
	}

	final NoParentPressImageView.OnCheckedChangeListener mCheckedChangeListener = new NoParentPressImageView.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(NoParentPressImageView buttonView, boolean isChecked) {
			if (mListener != null) {
				final int serverId = R.id.issues_listitem_tag_server_id;
				final int issueId = R.id.issues_listitem_tag_issue_id;
				mListener.onIssueFavoriteChanged((Long) buttonView.getTag(serverId), (Long) buttonView.getTag(issueId), isChecked);
			}
		}
	};

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		final ViewHolder viewHolder = (ViewHolder) view.getTag();
		final long issueId = Long.parseLong(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)));
		viewHolder.issueID.setText("#" + issueId);
		viewHolder.subject.setText(cursor.getString(cursor.getColumnIndex(IssuesDbAdapter.KEY_SUBJECT)));

		String version = null;
		String status = null;

		try {
			version = cursor.getString(cursor.getColumnIndex(IssuesDbAdapter.KEY_FIXED_VERSION));
		} catch (final Exception e) {
		}
		try {
			status = cursor.getString(cursor.getColumnIndex(IssuesDbAdapter.KEY_STATUS));
		} catch (final Exception e) {
		}

		if (TextUtils.isEmpty(version)) {
			version = mContext.getString(R.string.issue_version_na);
		} else {
			version = mContext.getString(R.string.issue_listview_target_version, version);
		}
		final String desc = cursor.getString(cursor.getColumnIndex(IssuesDbAdapter.KEY_DESCRIPTION));
		viewHolder.description.setText(desc);// Html.fromHtml(Util.htmlFromTextile(desc)));
		viewHolder.targetVersion.setText(version);
		viewHolder.status.setText(TextUtils.isEmpty(status) ? mContext.getString(R.string.issue_status_na) : status);
		viewHolder.project.setText(cursor.getString(cursor.getColumnIndex(IssuesDbAdapter.KEY_PROJECT)));

		// Customize project colors
		int projectId = cursor.getInt(cursor.getColumnIndex(IssuesDbAdapter.KEY_PROJECT_ID));
		viewHolder.project.setBackgroundResource(backgrounds[projectId % numColors]);
		viewHolder.project.setTextColor(textColors[projectId % numColors]);

		// Customize status colors
		int statusId = cursor.getInt(cursor.getColumnIndex(IssuesDbAdapter.KEY_STATUS_ID));
		viewHolder.status.setBackgroundResource(backgrounds[statusId % numColors]);
		viewHolder.status.setTextColor(textColors[statusId % numColors]);

		// Customize version colors
		int versionId = cursor.getInt(cursor.getColumnIndex(IssuesDbAdapter.KEY_FIXED_VERSION_ID));
		viewHolder.targetVersion.setBackgroundResource(backgrounds[versionId % numColors]);
		viewHolder.targetVersion.setTextColor(textColors[versionId % numColors]);

		// Limit height
		final AbsListView.LayoutParams lp;
		viewHolder.description.setVisibility(TextUtils.isEmpty(desc) ? View.GONE : View.VISIBLE);
		if (TextUtils.isEmpty(desc) || desc.length() < 50) {
			lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		} else {
			Resources res = mContext.getResources();
			lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) res.getDimension(R.dimen.issue_listitem_height));
		}
		viewHolder.layout.setLayoutParams(lp);

		// Set favorite touch delegate
		// Post in the parent's message queue to make sure the parent lays out its children before we call getHitRect()
		viewHolder.layout.post(new Runnable() {
			public void run() {
				if (viewHolder.favorite.getParent() != null && View.class.isInstance(viewHolder.favorite.getParent())) {
					Rect delegateArea = new Rect();
					viewHolder.favorite.getHitRect(delegateArea);
					delegateArea.top -= 48 * mDensity;
					delegateArea.bottom += 48 * mDensity;
					delegateArea.left -= 48 * mDensity;
					delegateArea.right += 48 * mDensity;
					// give the delegate to an ancestor of the view we're delegating the area to
					TouchDelegate expandedArea = new TouchDelegate(delegateArea, viewHolder.favorite);
					((View) viewHolder.favorite.getParent()).setTouchDelegate(expandedArea);
				}
			}
		});
		viewHolder.favorite.setTag(R.id.issues_listitem_tag_server_id, cursor.getLong(cursor.getColumnIndex(IssuesDbAdapter.KEY_SERVER_ID)));
		viewHolder.favorite.setTag(R.id.issues_listitem_tag_issue_id, issueId);
		final int isFav = cursor.getInt(cursor.getColumnIndex(IssuesDbAdapter.KEY_IS_FAVORITE));
		viewHolder.favorite.setChecked(isFav > 0);
		viewHolder.favorite.setOnCheckedChangeListener(mCheckedChangeListener);

		final boolean isClosed = "1".equals(cursor.getString(cursor.getColumnIndex(IssueStatusesDbAdapter.KEY_IS_CLOSED)));
		viewHolder.layout.setIsClosed(isClosed);
		if (isClosed) {
			viewHolder.subject.setPaintFlags(viewHolder.subject.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			viewHolder.issueID.setPaintFlags(viewHolder.subject.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		} else {
			viewHolder.subject.setPaintFlags(viewHolder.subject.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
			viewHolder.issueID.setPaintFlags(viewHolder.subject.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
		}
	}
}
