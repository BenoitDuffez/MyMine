package net.bicou.redmine.app.wiki;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.gson.Gson;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiPageFragment extends SherlockFragment {
	public static final String KEY_WIKI_PAGE = "net.bicou.redmine.WikiPage";
	public static final String KEY_WIKI_PAGE_NAME = "net.bicou.redmine.WikiPageName";

	long mProjectId;
	long mServerId;

	Project mProject;

	public static final String DEFAULT_PAGE_URI = "Wiki";
	/**
	 * Whether we shouldn't wait for the project spinner to trigger an update
	 */
	public static final String KEY_WIKI_DIRECT_LOAD = "net.bicou.redmine.WikiDirectLoad";

	ViewGroup mLayout;
	WikiPage mWikiPage;
	WebView mWebView;
	TextView mWikiTitle;

	/**
	 * The relative part. For example http://server.com/projects/1/wiki/TermsOfUse has a wiki page URI of TermsOfUse
	 */
	String mWikiPageURI;

	/**
	 * Human readable version of the wiki page URI
	 */
	String mWikiPageTitle;

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
		mWebView.setWebViewClient(new WikiWebViewClient());
		mProjectId = getArguments().getLong(Constants.KEY_PROJECT_ID);
		mServerId = getArguments().getLong(Constants.KEY_SERVER_ID);

		if (savedInstanceState != null) {
			mWikiPage = new Gson().fromJson(savedInstanceState.getString(KEY_WIKI_PAGE), WikiPage.class);
		}

		// Setup page URI and title
		mWikiPageURI = WikiPageLoader.getUriFromTitle(getArguments().getString(KEY_WIKI_PAGE_NAME));

		if (mWikiPage != null) {
			refreshUI();
		} else if (getArguments().getBoolean(KEY_WIKI_DIRECT_LOAD)) {
			triggerAsyncLoadWikiPage();
		} else {
			// empty fragment, need to call updateCurrentProject
		}

		return mLayout;
	}

	public void updateCurrentProject(final Project project) {
		L.d("");

		mWikiPageURI = "";
		mWikiPage = null;

		if (project != null) {
			mServerId = project.server.rowId;
			mProject = project;
			mProjectId = project.id;
		}

		L.i("serv=" + mServerId + " proj=" + mProjectId + "=" + mProject);
		L.i("wikipage=" + mWikiPage + "=" + (mWikiPage == null ? "null" : mWikiPage.text));

		//FIXME: doesn't properly switch from one project wiki to another

		// Load the page
		if (mWikiPage == null || TextUtils.isEmpty(mWikiPage.text)) {
			triggerAsyncLoadWikiPage();
		} else {
			refreshUI();
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_WIKI_PAGE, new Gson().toJson(mWikiPage));
		outState.putLong(Constants.KEY_PROJECT_ID, mProjectId);
	}

	private void triggerAsyncLoadWikiPage() {
		if (TextUtils.isEmpty(mWikiPageURI)) {
			mWikiPageURI = DEFAULT_PAGE_URI;
		}

		if (mWikiPageURI.equals("index")) {
			// TODO
		} else {
			getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
			new AsyncTask<Void, Void, WikiPage>() {
				@Override
				protected WikiPage doInBackground(final Void... params) {
					ServersDbAdapter sdb = new ServersDbAdapter(getActivity());
					sdb.open();
					Server server = sdb.getServer(mServerId);

					ProjectsDbAdapter pdb = new ProjectsDbAdapter(sdb);
					mProject = pdb.select(server, mProjectId);

					WikiDbAdapter db = new WikiDbAdapter(sdb);
					WikiPageLoader loader = new WikiPageLoader(server, getSherlockActivity(), db).enableCroutonNotifications(getSherlockActivity(), mLayout);
					WikiPage page = loader.actualSyncLoadWikiPage(mProject, mWikiPageURI);

					sdb.close();

					return page;
				}

				@Override
				protected void onPostExecute(final WikiPage wikiPage) {
					mWikiPage = wikiPage;
					final SherlockFragmentActivity act = getSherlockActivity();
					if (act != null) {
						refreshUI();
						act.setSupportProgressBarIndeterminateVisibility(false);
					}
				}
			}.execute();
		}
	}

	private void refreshUI() {
		if (mWikiPage == null || mWikiPage.text == null) {
			mWikiTitle.setText(R.string.wiki_empty_page);
			return;
		}

		SherlockFragmentActivity activity = getSherlockActivity();
		if (activity == null) {
			return;
		}

		if (TextUtils.isEmpty(mWikiPageTitle)) {
			activity.setTitle(R.string.wiki_title);
			mWikiTitle.setVisibility(View.GONE);
		} else {
			activity.setTitle(mWikiPageTitle);
			mWikiTitle.setVisibility(View.VISIBLE);
			if (TextUtils.isEmpty(mWikiPageURI)) {
				mWikiPageURI = "";
				mWikiPageTitle = DEFAULT_PAGE_URI;
			} else {
				try {
					mWikiPageTitle = URLDecoder.decode(mWikiPageURI, "UTF-8");
				} catch (final UnsupportedEncodingException e) {
					L.e("Unable to decode wiki page title: " + mWikiPageURI, e);
				}
			}
			mWikiTitle.setText(mWikiPageTitle);
		}

		// HTML Contents
		String html = Util.htmlFromTextile(mWikiPage.text);

		// [[linkTarget|Link name]]
		Pattern regex = Pattern.compile("\\[\\[([^\\|]+)\\|([^\\]]+)\\]\\]");
		Matcher matcher = regex.matcher(html);
		html = matcher.replaceAll("<a href=\"" + WikiPageLoader.getUrlPrefix(mProject, "") + "/wiki/$1\">$2</a>");

		// [[link]]
		regex = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
		matcher = regex.matcher(html);
		html = matcher.replaceAll("<a href=\"" + WikiPageLoader.getUrlPrefix(mProject, "") + "/wiki/$1\">$1</a>");

		// Issue
		// TODO
		// regex = Pattern.compile("#([0-9]+)([^;0-9]{1})");
		// matcher = regex.matcher(html);
		// html = matcher.replaceAll("<a href=\"" + getUrlPrefix("/issues/$1") + "\">#$1</a>$2");

		//mWebView.setVisibility(View.GONE);
		mWebView.loadData(html, "text/html; charset=UTF-8", null);
		//mWebView.reload();
		//mWebView.setVisibility(View.VISIBLE);
	}

	private class WikiWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
			if (mProject == null) {
				return true;
			}

			final int id = url.indexOf(WikiPageLoader.getUrlPrefix(mProject, "/wiki"));
			if (id >= 0) {
				final String newPage = url.substring(id + WikiPageLoader.getUrlPrefix(mProject, "/wiki/").length());
				final Bundle args = new Bundle();
				args.putString(KEY_WIKI_PAGE_NAME, newPage);
				args.putBoolean(KEY_WIKI_DIRECT_LOAD, true);
				args.putLong(Constants.KEY_PROJECT_ID, mProjectId);
				args.putLong(Constants.KEY_SERVER_ID, mServerId);
				final WikiPageFragment newWiki = WikiPageFragment.newInstance(args);
				getFragmentManager().beginTransaction().replace(android.R.id.content, newWiki).addToBackStack("prout").commit();
			} else {
				final Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				startActivity(intent);
			}
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			view.invalidate();
		}
	}
}
