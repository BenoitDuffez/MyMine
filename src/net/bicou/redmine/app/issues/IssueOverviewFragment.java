package net.bicou.redmine.app.issues;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.google.gson.Gson;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.wiki.WikiPageLoader;
import net.bicou.redmine.data.json.Attachment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueOverviewFragment extends SherlockFragment {
	TextView mSubject, mStatus, mPriority, mAssignee, mCategory, mTargetVersion;
	WebView mDescription;
	Issue mIssue;

	public static IssueOverviewFragment newInstance(final Bundle args) {
		final IssueOverviewFragment f = new IssueOverviewFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_issue_overview, container, false);
		L.d("");

		mSubject = (TextView) v.findViewById(R.id.issue_subject);
		mStatus = (TextView) v.findViewById(R.id.issue_status);
		mPriority = (TextView) v.findViewById(R.id.issue_priority);
		mAssignee = (TextView) v.findViewById(R.id.issue_assignee);
		mCategory = (TextView) v.findViewById(R.id.issue_category);
		mTargetVersion = (TextView) v.findViewById(R.id.issue_target_version);
		mDescription = (WebView) v.findViewById(R.id.issue_description);

		mIssue = new Gson().fromJson(getArguments().getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);

		mSubject.setText(mIssue.subject != null ? mIssue.subject : "");
		mStatus.setText(mIssue.status != null ? mIssue.status.name : "-");
		mPriority.setText(mIssue.priority != null ? mIssue.priority.name : "-");
		mAssignee.setText(mIssue.assigned_to != null ? mIssue.assigned_to.name : "-");
		mCategory.setText(mIssue.category != null ? mIssue.category.name : "-");
		mTargetVersion.setText(mIssue.fixed_version != null ? mIssue.fixed_version.name : "-");

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		AsyncTaskFragment.runTask(getSherlockActivity(), IssuesActivity.ACTION_ISSUE_OVERVIEW, null);
	}

	public String loadIssueOverview() {
		WikiDbAdapter db = new WikiDbAdapter(getActivity());
		db.open();
		WikiPageLoader loader = new WikiPageLoader(mIssue.server, getSherlockActivity(), db, null);

		String textile = mIssue.description != null ? mIssue.description : "";
		textile = loader.handleMarkupReplacements(mIssue.project, textile);

		IssuesDbAdapter idb = new IssuesDbAdapter(db);
		syncIssueAttachments(idb);
		textile = refactorImageUrls(idb, textile);

		db.close();
		return Util.htmlFromTextile(textile);
	}

	private void syncIssueAttachments(IssuesDbAdapter db) {
		// Sync attachments
		String url = "issues/" + mIssue.id + ".json";
		NameValuePair[] args = {
				new BasicNameValuePair("include", "attachments"),
		};

		Issue i = new JsonDownloader<Issue>(Issue.class).setStripJsonContainer(true).fetchObject(getActivity(), mIssue.server, url, args);
		if (i != null) {
			mIssue.attachments = i.attachments;
		}

		if (mIssue.attachments != null && mIssue.attachments.size() > 0) {
			for (Attachment attn : mIssue.attachments) {
				db.update(mIssue, attn);
			}
		}
	}

	private String refactorImageUrls(IssuesDbAdapter db, String textile) {
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
				attn = db.getAttnFromFileName(mIssue.server, path);
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
		mDescription.loadData(html, "text/html; charset=UTF-8", null);
	}
}
