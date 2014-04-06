package net.bicou.redmine.app.issues;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.app.issues.IssueFragment.FragmentActivationListener;
import net.bicou.redmine.data.json.Attachment;
import net.bicou.redmine.data.json.Issue;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class IssueAttachmentsFragment extends TrackedListFragment implements FragmentActivationListener {
	TextView mEmptyView;
	ViewGroup mLayout;

	private PullToRefreshLayout mPullToRefreshLayout;

	private List<Attachment> mAttachments;
	private Issue mIssue;
	private IssueItemsAdapter mAttachmentsAdapter;

	public static IssueAttachmentsFragment newInstance(final Bundle args) {
		final IssueAttachmentsFragment f = new IssueAttachmentsFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mIssue = new Gson().fromJson(getArguments().getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);
		mLayout = (ViewGroup) inflater.inflate(R.layout.frag_issue_journal, container, false);
		mEmptyView = (TextView) mLayout.findViewById(android.R.id.empty);
		return mLayout;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// This is the View which is created by ListFragment
		ViewGroup viewGroup = (ViewGroup) view;

		// We need to create a PullToRefreshLayout manually
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

		// Now setup the PullToRefreshLayout
		ActionBarPullToRefresh.from(getActivity()) //
				.insertLayoutInto(viewGroup).theseChildrenArePullable(android.R.id.list, android.R.id.empty) //
				// Set the OnRefreshListener
				.listener(new OnRefreshListener() {
					@Override
					public void onRefreshStarted(View view) {
						refreshIssueAttachments();
					}
				})
						// Finally commit the setup to our PullToRefreshLayout
				.setup(mPullToRefreshLayout);

		mAttachments = new ArrayList<Attachment>();
		mAttachmentsAdapter = new IssueItemsAdapter(getActivity(), mAttachments);

		final SwingBottomInAnimationAdapter adapter = new SwingBottomInAnimationAdapter(mAttachmentsAdapter);

		try {
			adapter.setAbsListView(getListView());
			setListAdapter(adapter);
			getListView().invalidate();
		} catch (IllegalStateException e) { // java.lang.IllegalStateException: Content view not yet created
			// Sometimes the screen rotation is too fast and the view is already disposed, the new one isn't created yet
			// TODO: maybe we could save the result and pass it when the listview is ready?
		}
	}

	@Override
	public void onFragmentActivated() {
		onAttachmentsLoaded();
	}

	private void refreshIssueAttachments() {
		AsyncTaskFragment.runTask((ActionBarActivity) getActivity(), IssuesActivity.ACTION_ISSUE_LOAD_ATTACHMENTS, mIssue);
	}

	public void onAttachmentsLoaded() {
		mPullToRefreshLayout.setRefreshComplete();
		mAttachments.clear();
		List<Attachment> attachments = ((IssuesActivity) getActivity()).getAttachments();
		if (attachments!=null){
			mAttachments.addAll(attachments);
		}
		mAttachmentsAdapter.notifyDataSetChanged();
	}

}
