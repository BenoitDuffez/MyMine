package net.bicou.redmine.app.wiki;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.util.L;

/**
 * Created by bicou on 19/07/13.
 */
public class WikiPagesListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final String KEY_PROJECT = "net.bicou;redmine.app.wiki.Project";
	WikiDbAdapter mWikiPagesDbAdapter;
	WikiPagesCursorAdapter mAdapter;
	View mFragmentView;

	public static WikiPagesListFragment newInstance(final Bundle args) {
		WikiPagesListFragment fragment = new WikiPagesListFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public void refreshList(final Project project) {
		Bundle args = new Bundle();
		args.putParcelable(KEY_PROJECT, project);
		L.d("refresh with " + args);
		getLoaderManager().restartLoader(0, args, this);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.d("");

		final Activity activity = getActivity();
		activity.setTitle(R.string.title_wiki_pages);

		mAdapter = new WikiPagesCursorAdapter(activity, null, true);
		setListAdapter(mAdapter);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mFragmentView = inflater.inflate(R.layout.frag_wikipages_list, container, false);
		return mFragmentView;
	}

	private WikiDbAdapter getHelper() {
		if (mWikiPagesDbAdapter == null) {
			mWikiPagesDbAdapter = new WikiDbAdapter(getActivity());
			mWikiPagesDbAdapter.open();
		}
		return mWikiPagesDbAdapter;
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		getSherlockActivity().setSupportProgressBarIndeterminate(true);
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
		Project project = args.getParcelable(KEY_PROJECT);
		return new WikiPagesListCursorLoader(getActivity(), getHelper(), project);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		mAdapter.swapCursor(data);

		L.d("refreshed, got " + data);

		if (getSherlockActivity() != null) {
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(data == null);
		}

		if (getListView() != null) {
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		}

		if (mFragmentView != null) {
			final TextView empty = (TextView) mFragmentView.findViewById(android.R.id.empty);
			if (empty != null && data != null && data.getCount() == 0) {
				empty.setText(R.string.no_wiki_pages);
			}
		}
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mWikiPagesDbAdapter != null) {
			mWikiPagesDbAdapter.close();
			mWikiPagesDbAdapter = null;
		}
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Cursor c = (Cursor) mAdapter.getItem(position);
		final String wikiPageUri = c.getString(c.getColumnIndex(WikiDbAdapter.KEY_TITLE));
		final long serverId = c.getLong(c.getColumnIndex(WikiDbAdapter.KEY_SERVER_ID));
		final long projectId = c.getLong(c.getColumnIndex(WikiDbAdapter.KEY_PROJECT_ID));

		final Bundle args = new Bundle();
		args.putString(WikiPageFragment.KEY_WIKI_PAGE_URI, wikiPageUri);
		args.putLong(Constants.KEY_SERVER_ID, serverId);
		args.putLong(Constants.KEY_PROJECT_ID, projectId);

		((WikiActivity) getActivity()).selectContent(args);
	}
}
