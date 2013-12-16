package net.bicou.redmine.app.issues;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;

import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedListFragment;
import net.bicou.redmine.app.issues.IssueFragment.FragmentActivationListener;
import net.bicou.redmine.app.issues.IssueHistoryDownloadTask.JournalsDownloadCallbacks;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueHistory;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.util.L;

import java.lang.reflect.Type;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class IssueHistoryFragment extends TrackedListFragment implements FragmentActivationListener {
	private IssueHistoryDownloadTask mUpdateTask;
	private Issue mIssue;
	TextView mEmptyView;
	ViewGroup mLayout;

	private IssueHistory mHistory;
	private static final String HISTORY_DATA = "net.bicou.redmine.app.issues.History";
	private PullToRefreshLayout mPullToRefreshLayout;

	public static IssueHistoryFragment newInstance(final Bundle args) {
		final IssueHistoryFragment f = new IssueHistoryFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mIssue = new Gson().fromJson(getArguments().getString(IssueFragment.KEY_ISSUE_JSON), Issue.class);

		mLayout = (ViewGroup) inflater.inflate(R.layout.frag_issue_journal, container, false);
		mEmptyView = (TextView) mLayout.findViewById(android.R.id.empty);

		if (savedInstanceState != null) {
			try {
				Type type = new TypeToken<IssueHistory>() {}.getType();
				mHistory = new Gson().fromJson(savedInstanceState.getString(HISTORY_DATA), type);
			} catch (Exception e) {
				L.e("Unable to deserialize saved issue history", e);
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
						refreshIssueHistory();
					}
				})
						// Finally commit the setup to our PullToRefreshLayout
				.setup(mPullToRefreshLayout);
	}

	@Override
	public void onResume() {
		super.onResume();
		setHistory(mHistory);
	}

	void setHistory(final IssueHistory history) {
		mUpdateTask = null;
		mHistory = history;
		if (mHistory == null || mHistory.journals == null || mHistory.journals.size() <= 0) {
			mEmptyView.setText(R.string.issue_history_none);
		} else {
			final IssueHistoryItemsAdapter journalAdapter = new IssueHistoryItemsAdapter(getActivity(), mIssue, mHistory);
			final SwingBottomInAnimationAdapter adapter = new SwingBottomInAnimationAdapter(journalAdapter);

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
			refreshIssueHistory();
		}
	}

	private void refreshIssueHistory() {
		mUpdateTask = new IssueHistoryDownloadTask((ActionBarActivity) getActivity(), new JournalsDownloadCallbacks() {
			@Override
			public void onPreExecute() {
				mEmptyView.setText(R.string.loading);
			}

			@Override
			public void onJournalsDownloaded(final IssueHistory history) {
				mPullToRefreshLayout.setRefreshComplete();
				setHistory(history);
			}

			@Override
			public void onJournalsFailed(final JsonNetworkError error) {
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
		outState.putString(HISTORY_DATA, new Gson().toJson(mHistory));
	}
}