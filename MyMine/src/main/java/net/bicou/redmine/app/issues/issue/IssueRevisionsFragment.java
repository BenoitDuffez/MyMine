package net.bicou.redmine.app.issues.issue;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.app.issues.issue.IssueFragment.FragmentActivationListener;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueJournal;
import net.bicou.redmine.data.json.IssueRevisions;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.util.L;

import java.lang.reflect.Type;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class IssueRevisionsFragment extends TrackedListFragment implements FragmentActivationListener {
	private IssueRevisionsDownloadTask mUpdateTask;
	private Issue mIssue;
	TextView mEmptyView;
	ViewGroup mLayout;

	private IssueRevisions mRevisions;
	private static final String REVISIONS_DATA = "net.bicou.redmine.app.issues.History.Revisions";
	private PullToRefreshLayout mPullToRefreshLayout;

	public static IssueRevisionsFragment newInstance(final Bundle args) {
		final IssueRevisionsFragment f = new IssueRevisionsFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mIssue = new Gson().fromJson(getArguments().getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);

		mLayout = (ViewGroup) inflater.inflate(R.layout.frag_issue_journal_revisions, container, false);
		mEmptyView = (TextView) mLayout.findViewById(android.R.id.empty);

		if (savedInstanceState != null) {
			try {
				Type type = new TypeToken<IssueJournal>() {}.getType();
				mRevisions = new Gson().fromJson(savedInstanceState.getString(REVISIONS_DATA), type);
			} catch (Exception e) {
				L.e("Unable to deserialize saved issue revisions", e);
			}
		}

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
						refreshIssueRevisions();
					}
				})
						// Finally commit the setup to our PullToRefreshLayout
				.setup(mPullToRefreshLayout);
	}

	@Override
	public void onResume() {
		super.onResume();
		setRevisions(mRevisions);
	}

	void setRevisions(final IssueRevisions revisions) {
		mUpdateTask = null;
		mRevisions = revisions;
		if (mRevisions == null || mRevisions.changesets == null || mRevisions.changesets.size() <= 0) {
			mEmptyView.setText(R.string.issue_history_none);
		} else {
			final IssueRevisionsAdapter journalAdapter = new IssueRevisionsAdapter(getActivity(), mRevisions.changesets);
			final com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter adapter = new com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter(journalAdapter);

			try {
				adapter.setAbsListView(getListView());
				setListAdapter(adapter);
				getListView().invalidate();
			} catch (IllegalStateException e) { // java.lang.IllegalStateException: Content view not yet created
				// Sometimes the screen rotation is too fast and the view is already disposed, the new one isn't created yet
				// TODO: maybe we could save the result and pass it when the listview is ready?
			}
		}
	}

	@Override
	public void onFragmentActivated() {
		if ((mIssue == null || mIssue.journals == null) && mUpdateTask == null) {
			refreshIssueRevisions();
		}
	}

	private void refreshIssueRevisions() {
		mUpdateTask = new IssueRevisionsDownloadTask((ActionBarActivity) getActivity(), new IssueRevisionsDownloadTask.RevisionsDownloadCallbacks() {
			@Override
			public void onPreExecute() {
				mEmptyView.setText(R.string.loading);
			}

			@Override
			public void onRevisionsDownloaded(final IssueRevisions revisions) {
				mPullToRefreshLayout.setRefreshComplete();
				setRevisions(revisions);
			}

			@Override
			public void onRevisionsDownloadFailed(final JsonNetworkError error) {
				mPullToRefreshLayout.setRefreshComplete();
				if (error == null) {
					Crouton.makeText(getActivity(), R.string.issue_journal_cant_download, Style.ALERT, mLayout).show();
				} else {
					error.displayCrouton(getActivity(), mLayout);
				}
			}
		}, mIssue);
		mUpdateTask.execute();
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(REVISIONS_DATA, new Gson().toJson(mRevisions));
	}
}
