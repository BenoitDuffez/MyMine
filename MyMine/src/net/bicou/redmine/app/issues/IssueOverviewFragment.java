package net.bicou.redmine.app.issues;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.gson.Gson;
import net.bicou.redmine.R;
import net.bicou.redmine.app.wiki.WikiPageFragment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

public class IssueOverviewFragment extends Fragment {
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

		String textile = mIssue.description != null ? mIssue.description : "";
		textile = WikiPageFragment.handleMarkupReplacements(mIssue.server, mIssue.project, (SherlockFragmentActivity) getActivity(), textile);
		mDescription.loadData(Util.htmlFromTextile(textile), "text/html; charset=UTF-8", null);

		return v;
	}
}
