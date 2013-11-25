package net.bicou.redmine.app.wiki;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.ViewGroup;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.util.L;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bicou on 18/06/13.
 * <p/>
 * Textile markup based text management
 */
public class WikiPageLoader {
	Server mServer;
	WikiDbAdapter mDb;
	Context mContext;

	ActionBarActivity mActivity;
	ViewGroup mCroutonHolder;

	/**
	 * Constructor.
	 * <p/>
	 * The activity and ViewGroup are required to display Crouton notifications in case of issues.
	 * <p/>
	 * The server and db may be required if the markup text includes wiki pages.
	 */
	public WikiPageLoader(Server server, Context context, WikiDbAdapter db) {
		mServer = server;
		mDb = db;
		mContext = context;
	}

	/**
	 * Use this to enable Crouton based notifications on your activity/viewgroup
	 */
	public WikiPageLoader enableCroutonNotifications(ActionBarActivity activity, ViewGroup croutonHolder) {
		mActivity = activity;
		mCroutonHolder = croutonHolder;
		return this;
	}

	/**
	 * Synchronously loads a wiki page, either from db if it exists, or from the server otherwise
	 */
	public WikiPage actualSyncLoadWikiPage(Project project, String uri) {
		// Try to load from DB first, if the sync is working it should already be there.
		WikiPage wikiPage = mDb.select(mServer, project, uri);

		if (wikiPage == null) {
			final String wikiPrefix = getUrlPrefix(project, "/wiki");
			if (TextUtils.isEmpty(wikiPrefix)) {
				if (mCroutonHolder != null && mActivity != null) {
					Crouton.makeText(mActivity, R.string.wiki_page_not_found, Style.ALERT, mCroutonHolder);
				}
				L.e("Can't find wiki prefix?!", null);
				return null;
			}
			final String wikiUri = TextUtils.isEmpty(uri) ? "" : "/" + uri;
			final String url = wikiPrefix + wikiUri + ".json";

			wikiPage = new JsonDownloader<WikiPage>(WikiPage.class).fetchObject(mContext, mServer, url);

			if (wikiPage == null) {
				return null;
			}
		}

		wikiPage.text = handleMarkupReplacements(project, wikiPage.text);
		return wikiPage;
	}

	/**
	 * Synchronously handle textile markup management. May load included pages from the db or the server.
	 */
	public String handleMarkupReplacements(final Project project, String text) {
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
				p = actualSyncLoadWikiPage(project, getUriFromTitle(m.group(1)));
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

	public static String getUriFromTitle(final String title) {
		if (TextUtils.isEmpty(title)) {
			return null;
		}
		return title.replace(" ", "_").replace(".", "");
	}

	public static String getUrlPrefix(final Project project, final String path) {
		if (project == null) {
			return null;
		}
		return "projects/" + project.identifier + path;
	}
}
