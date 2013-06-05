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
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AbsMyMineActivity;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiPageFragment extends SherlockFragment {
	public static final String KEY_WIKI_PAGE = "net.bicou.redmine.WikiPage";
	public static final String KEY_WIKI_PAGE_NAME = "net.bicou.redmine.WikiPageName";

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

		if (savedInstanceState != null) {
			mWikiPage = new Gson().fromJson(savedInstanceState.getString(KEY_WIKI_PAGE), WikiPage.class);
		}

		// Setup page URI and title
		mWikiPageURI = getUriFromTitle(getArguments().getString(KEY_WIKI_PAGE_NAME));

		if (getArguments().getBoolean(KEY_WIKI_DIRECT_LOAD)) {
			triggerAsyncLoadWikiPage();
		} else if (mWikiPage != null) {
			refreshUI();
		}

		return mLayout;
	}

	public void updateCurrentProject(final Project project) {
		L.d("");

		mWikiPageURI = "";
		mWikiPage = null;

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
	}

	private String getUrlPrefix(final String path) {
		return getUrlPrefix((AbsMyMineActivity) getActivity(), path);
	}

	private static String getUrlPrefix(final AbsMyMineActivity act, final String path) {
		final Project project = act.getCurrentProject();
		if (project == null) {
			return null;
		}
		return "projects/" + project.identifier + path;
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
					return actualSyncLoadWikiPage((AbsMyMineActivity) getActivity(), mWikiPageURI, mLayout);
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

	private static WikiPage actualSyncLoadWikiPage(final AbsMyMineActivity act, final String uri, final ViewGroup croutonHolder) {
		final Server server = act.getCurrentServer();
		final Project project = act.getCurrentProject();

		// Try to load from DB first, if the sync is working it should already be there.
		final WikiDbAdapter db = new WikiDbAdapter(act);
		db.open();
		WikiPage wikiPage = db.select(server, project, uri);
		db.close();

		if (wikiPage == null) {
			final String wikiPrefix = getUrlPrefix(act, "/wiki");
			if (TextUtils.isEmpty(wikiPrefix)) {
				if (croutonHolder != null) {
					Crouton.makeText(act, R.string.wiki_page_not_found, Style.ALERT, croutonHolder);
				}
				L.e("Can't find wiki prefix?!", null);
				return null;
			}
			final String wikiUri = TextUtils.isEmpty(uri) ? "" : "/" + uri;
			final String url = wikiPrefix + wikiUri + ".json";

			wikiPage = new JsonDownloader<WikiPage>(WikiPage.class).fetchObject(act, server, url);

			if (wikiPage == null) {
				return null;
			}
		}

		wikiPage.text = handleMarkupReplacements(act, wikiPage.text);
		return wikiPage;
	}

	public static String handleMarkupReplacements(final AbsMyMineActivity act, String text) {
		if (TextUtils.isEmpty(text)) {
			return "";
		}

		// Include pages
		final Pattern regex = Pattern.compile("\\{\\{include\\(([^\\)]+)\\)\\}\\}", 0);
		StringBuilder sb;
		WikiPage p;
		do {
			final Matcher m = regex.matcher(text);
			sb = new StringBuilder();
			if (m.find()) {
				sb.append(text.substring(0, m.start()));
				p = actualSyncLoadWikiPage(act, getUriFromTitle(m.group(1)), null);
				if (p != null && !TextUtils.isEmpty(p.text)) {
					sb.append("\n");
					sb.append(p.text);
					sb.append("\n");
				}
				sb.append(text.substring(m.end()));
				text = sb.toString();
			} else {
				break;
			}
		} while (true);

		return text;
	}

	private static String getUriFromTitle(final String title) {
		if (TextUtils.isEmpty(title)) {
			return null;
		}
		return title.replace(" ", "_").replace(".", "");
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
		html = matcher.replaceAll("<a href=\"" + getUrlPrefix("") + "/wiki/$1\">$2</a>");

		// [[link]]
		regex = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
		matcher = regex.matcher(html);
		html = matcher.replaceAll("<a href=\"" + getUrlPrefix("") + "/wiki/$1\">$1</a>");

		// Issue
		// TODO
		// regex = Pattern.compile("#([0-9]+)([^;0-9]{1})");
		// matcher = regex.matcher(html);
		// html = matcher.replaceAll("<a href=\"" + getUrlPrefix("/issues/$1") +
		// "\">#$1</a>$2");

		mWebView.loadData(html, "text/html; charset=UTF-8", null);
	}

	private class WikiWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
			final int id = url.indexOf(getUrlPrefix("/wiki"));
			if (id >= 0) {
				final String newPage = url.substring(id + getUrlPrefix("/wiki/").length());
				final Bundle args = new Bundle();
				args.putString(KEY_WIKI_PAGE_NAME, newPage);
				args.putBoolean(KEY_WIKI_DIRECT_LOAD, true);
				final WikiPageFragment newWiki = WikiPageFragment.newInstance(args);
				getFragmentManager().beginTransaction().replace(R.id.wiki_contents, newWiki).addToBackStack("prout").commit();
			} else {
				final Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				startActivity(intent);
			}
			return true;
		}
	}
}
