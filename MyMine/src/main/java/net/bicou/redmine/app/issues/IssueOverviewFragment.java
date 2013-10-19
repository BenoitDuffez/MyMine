package net.bicou.redmine.app.issues;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.app.wiki.WikiPageLoader;
import net.bicou.redmine.app.wiki.WikiUtils;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Attachment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueOverviewFragment extends TrackedFragment {
	TextView mTrackerAndId, mSubject, mStatus, mPriority, mAssignee, mCategory, mTargetVersion, mStartDate, mDueDate, mPercentDone, mSpentTime, mAuthor, mParent;
	ImageView mAuthorAvatar, mAssignedAvatar;
	CheckBox mFavorite;
	WebView mDescription;
	Issue mIssue;
	static java.text.DateFormat mLongDateFormat;
	static java.text.DateFormat mTimeFormat;
	WikiUtils.WikiWebViewClient mClient;
	ViewGroup mMainLayout;

	public static IssueOverviewFragment newInstance(final Bundle args) {
		final IssueOverviewFragment f = new IssueOverviewFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_issue_overview, container, false);
		L.d("");

		mMainLayout = (ViewGroup) v.findViewById(R.id.issue_main_layout);
		mTrackerAndId = (TextView) v.findViewById(R.id.issue_tracker_id);
		mSubject = (TextView) v.findViewById(R.id.issue_subject);
		mStatus = (TextView) v.findViewById(R.id.issue_status);
		mPriority = (TextView) v.findViewById(R.id.issue_priority);
		mTargetVersion = (TextView) v.findViewById(R.id.issue_target_version);
		mCategory = (TextView) v.findViewById(R.id.issue_category);
		mDescription = (WebView) v.findViewById(R.id.issue_description);
		mStartDate = (TextView) v.findViewById(R.id.issue_start_date);
		mDueDate = (TextView) v.findViewById(R.id.issue_due_date);
		mPercentDone = (TextView) v.findViewById(R.id.issue_percent_done);
		mSpentTime = (TextView) v.findViewById(R.id.issue_spent_time);
		mAuthor = (TextView) v.findViewById(R.id.issue_author);
		mAuthorAvatar = (ImageView) v.findViewById(R.id.issue_author_avatar);
		mAssignee = (TextView) v.findViewById(R.id.issue_assignee);
		//		mAssignedAvatar=v.findViewById(R.id.issue_assign)
		mParent = (TextView) v.findViewById(R.id.issue_parent);
		mFavorite = (CheckBox) v.findViewById(R.id.issue_is_favorite);

		mFavorite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				mIssue.is_favorite = isChecked;//TODO: may crash if the phone is ass-lagging and takes 1 minute to display the fucking activity: NPE...
				IssuesDbAdapter db = new IssuesDbAdapter(getActivity());
				db.open();
				db.update(mIssue);
				db.close();
			}
		});

		mMainLayout.setVisibility(View.INVISIBLE);

		mClient = new WikiUtils.WikiWebViewClient(getSherlockActivity());
		WebSettings settings = mDescription.getSettings();
		settings.setDefaultTextEncodingName("utf-8");
		mDescription.setWebViewClient(mClient);

		return v;
	}

	@Override
	public void onViewStateRestored(final Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		AsyncTaskFragment.runTask(getSherlockActivity(), IssuesActivity.ACTION_ISSUE_LOAD_ISSUE, savedInstanceState == null ? getArguments() : savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(Constants.KEY_SERVER_ID, getArguments().getLong(Constants.KEY_SERVER_ID));
		outState.putLong(Constants.KEY_ISSUE_ID, getArguments().getLong(Constants.KEY_ISSUE_ID));
		outState.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(mIssue, Issue.class));
	}

	public static Issue loadIssue(Context context, Bundle args) {
		Issue issue = new Gson().fromJson(args.getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
		if (issue != null) {
			return issue;
		}

		// Load from the server, because there's a possibility it's not been synced
		ServersDbAdapter sdb = new ServersDbAdapter(context);
		sdb.open();
		Server server = sdb.getServer(args.getLong(Constants.KEY_SERVER_ID));
		IssuesDbAdapter idb = new IssuesDbAdapter(sdb);

		String uri = "issues/" + args.getLong(Constants.KEY_ISSUE_ID) + ".json";
		JsonDownloader<Issue> downloader = new JsonDownloader<Issue>(Issue.class).setStripJsonContainer(true);
		issue = downloader.fetchObject(context, server, uri);

		if (issue != null) {
			issue.server = server;
			idb.update(issue);
		}
		sdb.close();

		return issue;
	}

	public void onIssueLoaded(Issue result) {
		if (result == null) {
			return;
		}

		mIssue = result;
		mIssue.project.server = mIssue.server;
		mClient.setProject(mIssue.project);

		// Trigger UI update
		AsyncTaskFragment.runTask(getSherlockActivity(), IssuesActivity.ACTION_ISSUE_LOAD_OVERVIEW, mIssue);
		AsyncTaskFragment.runTask(getSherlockActivity(), IssuesActivity.ACTION_ISSUE_LOAD_ATTACHMENTS, mIssue);

		// Update UI
		mMainLayout.setVisibility(View.VISIBLE);

		mTrackerAndId.setText(getString(R.string.issue_tracker_id_format, mIssue.tracker.name, mIssue.id));
		mSubject.setText(mIssue.subject != null ? mIssue.subject : "");
		mStatus.setText(mIssue.status != null ? mIssue.status.name : "-");
		mPriority.setText(mIssue.priority != null ? mIssue.priority.name : "-");
		mCategory.setText(mIssue.category != null ? mIssue.category.name : "-");
		mTargetVersion.setText(mIssue.fixed_version != null ? mIssue.fixed_version.name : "-");
		java.text.DateFormat format = DateFormat.getMediumDateFormat(getActivity());
		mStartDate.setText(Util.isEpoch(mIssue.start_date) ? "" : format.format(mIssue.start_date.getTime()));
		mDueDate.setText(Util.isEpoch(mIssue.due_date) ? "" : format.format(mIssue.due_date.getTime()));
		mPercentDone.setText(String.format("%d%%", mIssue.done_ratio));
		mSpentTime.setText(getString(R.string.issue_spent_time_format, mIssue.spent_hours));
		mIssue.author = displayNameAndAvatar(getActivity(), mIssue, mAuthor, mAuthorAvatar, mIssue.author, getString(R.string.issue_author_name_format),
				mIssue.created_on);
		mIssue.assigned_to = displayNameAndAvatar(getActivity(), mIssue, mAssignee, mAssignedAvatar, mIssue.assigned_to, "%1$s", null); // TODO
		mParent.setText(mIssue.parent != null && mIssue.parent.id > 0 ? Long.toString(mIssue.parent.id) : "");
		mFavorite.setChecked(mIssue.is_favorite);
	}

	public static User displayNameAndAvatar(Context context, Issue issue, TextView name, ImageView avatar, User user, String textResId, Calendar date) {
		if (user == null || user.id <= 0) {
			if (name != null) {
				name.setText("");
			}
			if (avatar != null) {
				avatar.setVisibility(View.INVISIBLE);
			}
			return null;
		}

		UsersDbAdapter db = new UsersDbAdapter(context);
		db.open();
		User u = db.select(issue.server, user.id);
		db.close();

		if (u != null) {
			if (u.createGravatarUrl() && avatar != null) {
				ImageLoader.getInstance().displayImage(u.gravatarUrl, avatar);
				avatar.setVisibility(View.VISIBLE);
			} else if (avatar != null) {
				avatar.setVisibility(View.INVISIBLE);
			}

			if (name != null) {
				String formattedDate;
				if (date != null && date.getTimeInMillis() > 10000) {
					long delta = (new GregorianCalendar().getTimeInMillis() - date.getTimeInMillis()) / 1000;
					if (delta < 60) {
						formattedDate = context.getString(R.string.time_delay_moments);
					} else if (delta < 3600) {
						formattedDate = MessageFormat.format(context.getString(R.string.time_delay_minutes), (int) (delta / 60));
					} else if (delta < 3600 * 24) {
						formattedDate = MessageFormat.format(context.getString(R.string.time_delay_hours), (int) (delta / 3600));
					} else if (delta < 3600 * 24 * 30) {
						formattedDate = MessageFormat.format(context.getString(R.string.time_delay_days), (int) (delta / (3600 * 24)));
					} else if (delta < 3600 * 24 * 365) {
						formattedDate = MessageFormat.format(context.getString(R.string.time_delay_months), (int) (delta / (3600 * 24 * 30)));
					} else {
						formattedDate = MessageFormat.format(context.getString(R.string.time_delay_years), (int) (delta / (3600 * 24 * 365)));
					}
				} else {
					formattedDate = "";
				}

				name.setText(String.format(textResId, u.getName(), formattedDate));
				name.setOnClickListener(mDatePopupClickListener);
				name.setTag(date);
			}
		}

		return u;
	}

	private static View.OnClickListener mDatePopupClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			Object tag = view.getTag();
			if (tag != null && tag instanceof Calendar) {
				Date date = ((Calendar) tag).getTime();
				String fullDate = mLongDateFormat.format(date) + " " + mTimeFormat.format(date);
				Toast.makeText(view.getContext(), fullDate, Toast.LENGTH_LONG).show();
			}
		}
	};

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		mLongDateFormat = DateFormat.getLongDateFormat(activity);
		mTimeFormat = DateFormat.getTimeFormat(activity);
	}

	public static String loadIssueOverview(Context context, Issue issue) {
		WikiDbAdapter db = new WikiDbAdapter(context);
		db.open();
		WikiPageLoader loader = new WikiPageLoader(issue.server, context, db);

		String textile = issue.description != null ? issue.description : "";
		textile = loader.handleMarkupReplacements(issue.project, textile);

		db.close();
		return WikiUtils.htmlFromTextile(textile);
	}

	public static Object loadIssueAttachments(Context context, Issue issue) {
		IssuesDbAdapter db = new IssuesDbAdapter(context);
		db.open();
		String url = "issues/" + issue.id + ".json";
		NameValuePair[] args = {
				new BasicNameValuePair("include", "attachments"),
		};

		String textile = issue.description;

		JsonNetworkError error = null;
		JsonDownloader<Issue> downloader = new JsonDownloader<Issue>(Issue.class).setStripJsonContainer(true);
		Issue issueWithAttns = downloader.fetchObject(context, issue.server, url, args);
		if (issueWithAttns != null) {
			issueWithAttns.server = issue.server;
			if (issueWithAttns.attachments != null && issueWithAttns.attachments.size() > 0) {
				for (Attachment attn : issueWithAttns.attachments) {
					db.update(issueWithAttns, attn);
				}
			}
			textile = refactorImageUrls(db, issue.server, textile);
		} else {
			error = downloader.getError();
			if (error != null && error.httpResponseCode == 404) {
				// 404 on an issue means it's been deleted
				L.d("404 on issue, it's been deleted. Delete from db: " + db.delete(issue));
			}
			textile = null;
		}

		db.close();

		if (error != null) {
			return error;
		} else {
			return WikiUtils.htmlFromTextile(textile);
		}
	}

	public void onNetworkError(final JsonNetworkError error) {
		if (error != null) {
			if (error.httpResponseCode == 404) {
				// 404 on an issue means it's been deleted
				Crouton.makeText(getActivity(), getString(R.string.issue_deleted_from_server), Style.INFO).show();
			} else {
				error.displayCrouton(getActivity(), null);
			}
		}

		getSherlockActivity().getSupportFragmentManager().popBackStack();
		IssuesListFragment fragment = ((IssuesActivity) getActivity()).getMainFragment();
		if (fragment != null) {
			fragment.refreshList();
		}
	}

	private static String refactorImageUrls(IssuesDbAdapter db, Server server, String textile) {
		if (TextUtils.isEmpty(textile)) {
			return "";
		}

		Pattern regex = Pattern.compile("(!>?)([^!]+)!", 0);
		Matcher m = regex.matcher(textile);
		String path;
		Attachment attn;
		List<String> matches = new ArrayList<String>();
		List<String> replacements = new ArrayList<String>();

		while (m.find()) {
			L.d("found image: " + m.group(0));

			path = m.group(2);
			if (path.startsWith(">")) {
				path = path.substring(1);
			}
			if (!path.startsWith("http://") && !path.startsWith("https://") && !path.startsWith("ftp://")) {
				attn = db.getAttnFromFileName(server, path);
				if (attn != null && !TextUtils.isEmpty(attn.content_url)) {
					matches.add(m.group(0));
					replacements.add(m.group(1) + attn.content_url + "!");
					L.i("replaced url to: " + m.group(1) + attn.content_url + "!");
				}
			}
		}

		for (int i = 0; i < matches.size(); i++) {
			textile = textile.replace(matches.get(i), replacements.get(i));
		}

		return textile;
	}

	public void onIssueOverviewLoaded(String html) {
		mDescription.loadData(html, "text/html; charset=UTF-8", "UTF-8");
	}
}
