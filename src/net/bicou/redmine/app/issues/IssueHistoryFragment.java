package net.bicou.redmine.app.issues;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.R;
import net.bicou.redmine.app.issues.IssueFragment.FragmentActivationListener;
import net.bicou.redmine.app.issues.IssueHistoryDownloadTask.JournalsDownloadCallbacks;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueHistory;
import net.bicou.redmine.util.L;

import java.lang.reflect.Type;

public class IssueHistoryFragment extends SherlockListFragment implements FragmentActivationListener {
	private IssueHistoryDownloadTask mUpdateTask;
	private Issue mIssue;
	TextView mEmptyView;
	ViewGroup mLayout;

	private IssueHistory mHistory;
	private static final String HISTORY_DATA = "net.bicou.redmine.app.issues.History";

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
				Type type = new TypeToken<IssueHistory>() {
				}.getType();
				setHistory((IssueHistory) new Gson().fromJson(savedInstanceState.getString(HISTORY_DATA), type));
			} catch (Exception e) {
				L.e("Unable to deserialize saved issue history", e);
			}
		}

		return mLayout;
	}

	void setHistory(final IssueHistory history) {
		mUpdateTask = null;
		mHistory = history;
		if (mHistory == null || mHistory.journals == null || mHistory.journals.size() <= 0) {
			mEmptyView.setText(R.string.issue_history_none);
		} else {
			final IssueHistoryItemsAdapter journalAdapter = new IssueHistoryItemsAdapter(getActivity(), mIssue, mHistory);
			final SwingBottomInAnimationAdapter adapter = new SwingBottomInAnimationAdapter(journalAdapter);
			adapter.setListView(getListView());
			setListAdapter(adapter);
			getListView().invalidate();
		}
	}

	@Override
	public void onFragmentActivated() {
		if ((mIssue == null || mIssue.journals == null) && mUpdateTask == null) {
			mUpdateTask = new IssueHistoryDownloadTask(getSherlockActivity(), new JournalsDownloadCallbacks() {
				@Override
				public void onPreExecute() {
					getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
				}

				@Override
				public void onJournalsDownloaded(final IssueHistory history) {
					setHistory(history);
					if (history == null || history.journals == null) {
						Crouton.makeText(getSherlockActivity(), R.string.issue_journal_cant_download, Style.ALERT, mLayout).show();
					}
					getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
				}
			}, mIssue);
			mUpdateTask.execute();
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(HISTORY_DATA, new Gson().toJson(mHistory));
	}
}
