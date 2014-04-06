package net.bicou.redmine.app.issues;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.util.L;

import java.util.Locale;

public class IssueFragment extends TrackedFragment {
	public static final String KEY_ISSUE_JSON = "net.bicou.redmine.Issue";
	public static final String KEY_ATTACHMENTS_JSON = "net.bicou.redmine.Issue.Attachments";

	private Issue mIssue;

	private IssueTabsAdapter mAdapter;

	private static final int NB_TABS = 3;
	public static final int FRAGMENT_OVERVIEW = 0;
	public static final int FRAGMENT_HISTORY = 1;
	public static final int FRAGMENT_ATTACHMENTS = 2;
	private static final String[] TAB_TITLES = new String[NB_TABS];

	public interface FragmentActivationListener {
		void onFragmentActivated();
	}

	public static IssueFragment newInstance(final Bundle args) {
		final IssueFragment f = new IssueFragment();
		f.setArguments(args);
		return f;
	}

	private static class IssueTabsAdapter extends FragmentPagerAdapter {
		Fragment[] mFragments = new Fragment[NB_TABS];
		Bundle args;

		public IssueTabsAdapter(final FragmentManager fm, final Bundle args) {
			super(fm);
			this.args = args;
			Bundle hack = new Bundle();
			try {
				for (int i = 0; i < NB_TABS; i++) {
					hack.putInt("hack", i);
					mFragments[i] = fm.getFragment(hack, "hack");
				}
			} catch (Exception e) {
				// No need to fail here
			}
		}

		@Override
		public int getCount() {
			return NB_TABS;
		}

		@Override
		public CharSequence getPageTitle(final int position) {
			return TAB_TITLES[position].toUpperCase(Locale.getDefault());
		}

		/**
		 * {{@link #getItem(int)} is a VERY bad name: it is not meant to retrieve the item, but it is meant to INSTANTIATE the item.<br /> Hence,
		 * this method will simply retrieve the item.
		 *
		 * @return The {@code Fragment} at that position
		 */
		public Fragment getFragment(final int position) {
			return mFragments[position];
		}

		@Override
		public Fragment getItem(final int position) {
			switch (position) {
			case FRAGMENT_OVERVIEW:
				mFragments[position] = IssueOverviewFragment.newInstance(args);
				break;
			case FRAGMENT_HISTORY:
				mFragments[position] = IssueHistoryFragment.newInstance(args);
				break;
			case FRAGMENT_ATTACHMENTS:
				mFragments[position] = IssueAttachmentsFragment.newInstance(args);
				break;
			}
			return mFragments[position];
		}
	}

	public Fragment getFragmentFromViewPager(int position) {
		return mAdapter == null ? null : mAdapter.getFragment(position);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_issue, container, false);
		L.d("");

		int i = 0;
		TAB_TITLES[i++] = getString(R.string.issue_overview_title);
		TAB_TITLES[i++] = getString(R.string.issue_journal_title);
		TAB_TITLES[i++] = getString(R.string.issue_attachments_title);

		final Bundle args = new Bundle();
		if (getArguments() != null) {
			args.putAll(getArguments());
		}
		if (savedInstanceState == null) {
			// Load issue
			if (args.containsKey(KEY_ISSUE_JSON)) {
				mIssue = new Gson().fromJson(args.getString(KEY_ISSUE_JSON), Issue.class);
			} else {
				final ServersDbAdapter sdb = new ServersDbAdapter(getActivity());
				sdb.open();
				final Server server = sdb.getServer(args.getLong(Constants.KEY_SERVER_ID, 0));

				if (server == null) {
					L.e("Server can't be null now!", null);
					return v;
				}

				final IssuesDbAdapter db = new IssuesDbAdapter(sdb);
				mIssue = db.select(server, args.getLong(Constants.KEY_ISSUE_ID), null);
				db.close();
			}
		} else {
			mIssue = new Gson().fromJson(savedInstanceState.getString(KEY_ISSUE_JSON), Issue.class);
		}
		args.putString(KEY_ISSUE_JSON, new Gson().toJson(mIssue));

		// Adapter
		mAdapter = new IssueTabsAdapter(getChildFragmentManager(), args);

		// Listener
		OnPageChangeListener listener = new OnPageChangeListener() {
			@Override
			public void onPageSelected(final int position) {
				final Fragment f = mAdapter.getFragment(position);
				if (f instanceof FragmentActivationListener) {
					((FragmentActivationListener) f).onFragmentActivated();
				}
			}

			@Override
			public void onPageScrolled(final int arg0, final float arg1, final int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(final int arg0) {
			}
		};

		// Bind everything
		final ViewPager pager = (ViewPager) v.findViewById(R.id.issue_pager);
		pager.setAdapter(mAdapter);
		pager.setOnPageChangeListener(listener);

		setHasOptionsMenu(true);

		return v;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mIssue != null) {
			final String json = new Gson().toJson(mIssue);
			outState.putString(KEY_ISSUE_JSON, json);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().supportInvalidateOptionsMenu();
	}

	public Issue getIssue() {
		return mIssue;
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_issue, menu);
	}
}
