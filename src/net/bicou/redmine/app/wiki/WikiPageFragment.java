package net.bicou.redmine.app.wiki;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.util.L;

public class WikiPageFragment extends SherlockFragment {
	public static final String KEY_WIKI_PAGE = "net.bicou.redmine.WikiPage";
	public static final String KEY_WIKI_PAGE_URI = "net.bicou.redmine.WikiPageName";

	Project mProject;

	public static final String DEFAULT_PAGE_URI = "Wiki";

	ViewGroup mLayout;
	WikiPage mWikiPage;
	WebView mWebView;
	TextView mWikiTitle;
	CheckBox mFavorite;
	String mHtmlContents;

	WikiUtils.WikiWebViewClient mClient;

	/**
	 * The relative part. For example http://server.com/projects/1/wiki/TermsOfUse has a wiki page URI of TermsOfUse
	 */
	String mWikiPageURI;

	public static final int ACTION_LOAD_WIKI_PAGE = 0;

	public static class WikiPageLoadParameters {
		public String uri;
		public Project project;
		public SherlockFragmentActivity croutonActivity;
		public ViewGroup croutonLayout;
		public WikiPage wikiPage;
		public String resultHtml;
		public long projectId;
		public long serverId;
	}

	public static WikiPageFragment newInstance(final Bundle args) {
		final WikiPageFragment f = new WikiPageFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		L.d("");
		mLayout = (ViewGroup) inflater.inflate(R.layout.frag_wikipage, container, false);
		mWebView = (WebView) mLayout.findViewById(R.id.wiki_page);
		mWikiTitle = (TextView) mLayout.findViewById(R.id.wiki_title);
		mFavorite = (CheckBox) mLayout.findViewById(R.id.wiki_favorite);

		mClient = new WikiUtils.WikiWebViewClient(getSherlockActivity());
		mWebView.setWebViewClient(mClient);

		mFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				mWikiPage.is_favorite = !mWikiPage.is_favorite;
				WikiDbAdapter db = new WikiDbAdapter(getActivity());
				db.open();
				db.update(mWikiPage);
				db.close();
			}
		});

		long projectId = getArguments().getLong(Constants.KEY_PROJECT_ID);
		long serverId = getArguments().getLong(Constants.KEY_SERVER_ID);
		mWikiPageURI = getArguments().getString(KEY_WIKI_PAGE_URI);

		if (savedInstanceState != null) {
			mWikiPage = new Gson().fromJson(savedInstanceState.getString(KEY_WIKI_PAGE), WikiPage.class);
			mProject = savedInstanceState.getParcelable(Constants.KEY_PROJECT);
		} else if (getArguments().keySet().contains(KEY_WIKI_PAGE)) {
			mWikiPage = new Gson().fromJson(getArguments().getString(KEY_WIKI_PAGE), WikiPage.class);
			mProject = getArguments().getParcelable(Constants.KEY_PROJECT);
		}

		WikiPageLoadParameters params = new WikiPageLoadParameters();
		params.wikiPage = mWikiPage;
		params.project = mProject;
		params.uri = mWikiPageURI;
		params.croutonLayout = mLayout;
		params.croutonActivity = getSherlockActivity();
		params.serverId = serverId;
		params.projectId = projectId;
		AsyncTaskFragment.runTask(getSherlockActivity(), ACTION_LOAD_WIKI_PAGE, params);

		setHasOptionsMenu(true);

		return mLayout;
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_wiki_page, menu);
	}

	@Override
	public void onPrepareOptionsMenu(final Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem item = menu.findItem(R.id.menu_wiki_fullscreen);
		if (item != null) {
			item.setVisible(getActivity() instanceof WikiActivity && ((WikiActivity) getActivity()).isSplitScreen());
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_wiki_fullscreen:
			Intent intent = new Intent(getActivity(), WikiPageActivity.class);
			intent.putExtras(saveInstanceState(getArguments()));
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		saveInstanceState(outState);
	}

	private Bundle saveInstanceState(Bundle outState) {
		if (outState != null && mWikiPage != null) {
			outState.putString(KEY_WIKI_PAGE, new Gson().toJson(mWikiPage));
			outState.putParcelable(Constants.KEY_PROJECT, mWikiPage.project);
			outState.putLong(Constants.KEY_PROJECT_ID, mWikiPage.project.id);
			outState.putLong(Constants.KEY_SERVER_ID, mWikiPage.project.server.rowId);
		}
		return outState;
	}

	/**
	 * Loads the wiki page and returns its text in HTML format, ready to be loaded in the WebView
	 * <p/>
	 * If {@code params.wikiPage} is valid, the HTML is directly returned from its textile markup.<br /> Otherwise,
	 * this will try to load the wiki page from its URI and
	 * project/server
	 */
	public static WikiPageLoadParameters loadWikiPage(Context context, WikiPageLoadParameters params) {
		if (params == null) {
			return null;
		}

		if (params.wikiPage == null || TextUtils.isEmpty(params.wikiPage.text)) {
			if (TextUtils.isEmpty(params.uri)) {
				params.uri = DEFAULT_PAGE_URI;
			}

			if (params.uri.equals("index")) {
				// TODO
			} else {
				ProjectsDbAdapter db = new ProjectsDbAdapter(context);
				db.open();

				// Don't load project if it's already loaded
				if (params.wikiPage != null && params.wikiPage.project != null && params.wikiPage.project.id > 0) {
					params.project = params.wikiPage.project;
				} else {
					params.project = db.select(params.serverId, params.projectId, null);
				}

				WikiDbAdapter wdb = new WikiDbAdapter(db);

				WikiPageLoader loader = new WikiPageLoader(params.project.server, context, wdb) //
						.enableCroutonNotifications(params.croutonActivity, params.croutonLayout);
				params.wikiPage = loader.actualSyncLoadWikiPage(params.project, params.uri);

				db.close();
			}
		}

		if (params.project == null && params.wikiPage != null && params.wikiPage.project != null) {
			params.project = params.wikiPage.project;
		}

		if (params.wikiPage == null || TextUtils.isEmpty(params.wikiPage.text)) {
			return null;
		}

		params.resultHtml = WikiUtils.htmlFromTextile(params.wikiPage.text);

		return params;
	}

	public void refreshUI(WikiPageLoadParameters result) {
		if (result == null) {
			// TODO: result==null means non existing wiki page which means we should popup a create wiki page dialog
			return;
		}
		mWikiPage = result.wikiPage;
		mHtmlContents = result.resultHtml;
		mProject = result.project;

		// Null checks
		if (mWikiPage == null || mWikiPage.text == null) {
			mWikiTitle.setText(result == null ? R.string.wiki_page_not_found : R.string.wiki_empty_page);
			mWikiTitle.setVisibility(View.VISIBLE);
			mFavorite.setVisibility(View.GONE);
			mWebView.setVisibility(View.GONE);
			return;
		}

		mClient.setProject(mProject);

		// Title
		String wikiPageTitle = mWikiPage.title;
		if (TextUtils.isEmpty(wikiPageTitle)) {
			mWikiTitle.setVisibility(View.GONE);
		} else {
			mWikiTitle.setVisibility(View.VISIBLE);
			if (TextUtils.isEmpty(mWikiPageURI)) {
				mWikiPageURI = "";
				wikiPageTitle = DEFAULT_PAGE_URI;
			} else {
				wikiPageTitle = WikiUtils.titleFromUri(mWikiPageURI);
			}
			if (TextUtils.isEmpty(wikiPageTitle)) {
				mWikiTitle.setVisibility(View.GONE);
			} else {
				mWikiTitle.setVisibility(View.VISIBLE);
				mWikiTitle.setText(wikiPageTitle);
			}
		}

		// Favorite icon state
		mFavorite.setChecked(mWikiPage.is_favorite);

		// HTML Contents
		mWebView.loadData(mHtmlContents, "text/html; charset=UTF-8", null);
	}
}
