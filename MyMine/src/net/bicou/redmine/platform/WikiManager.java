package net.bicou.redmine.platform;

import android.accounts.Account;
import android.content.Context;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.WikiPage;
import net.bicou.redmine.data.sqlite.WikiDbAdapter;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class WikiManager {
	final class WikiPageJsonContainer {
		public WikiPage wiki_page;
	}

	public static synchronized long updateWiki(final Context context, final Account account, final Server server, final Project project,
			final List<WikiPage> remoteList, final long lastSyncMarker) {
		L.d("ctx=" + context + ", account=" + account + ", remote wiki pages count=" + remoteList.size() + " syncMarker=" + lastSyncMarker);
		final WikiDbAdapter db = new WikiDbAdapter(context);
		db.open();
		final List<WikiPage> localPages = db.selectAll(server, project);

		long currentSyncMarker = lastSyncMarker;
		long pageUpdateDate;
		WikiPage localPage;
		final Calendar lastKnownChange = new GregorianCalendar();
		lastKnownChange.setTimeInMillis(lastSyncMarker);

		WikiPageJsonContainer tmp;

		for (final WikiPage page : remoteList) {
			localPage = null;
			for (final WikiPage lp : localPages) {
				if (lp != null && lp.title != null && page != null && page.title != null && lp.title.compareTo(page.title) == 0) {
					localPage = lp;
					break;
				}
			}

			// Download missing parts of that page
			final String url = String.format(Locale.ENGLISH, "projects/%d/wiki/%s.json", project.id, page.title);
			tmp = new JsonDownloader<WikiPageJsonContainer>(WikiPageJsonContainer.class).fetchObject(context, server, url);
			if (tmp != null) {
				page.text = tmp.wiki_page.text;
				page.comments = tmp.wiki_page.comments;
				page.author.id = tmp.wiki_page.author.id;
			}

			page.server = server;
			page.project = project;

			if (localPage == null) {
				// New, add it
				db.insert(server, project, page);
			} else {
				pageUpdateDate = page.updated_on.getTimeInMillis();

				// Save current sync marker
				if (pageUpdateDate > currentSyncMarker) {
					currentSyncMarker = pageUpdateDate;
				}

				// Check status
				if (localPage.updated_on.before(page.updated_on) || localPage.updated_on.getTimeInMillis() < 1369612800000L) { // HACK: force update for dates < 27/05/2013 00:00:00
					// Outdated, update it
					db.update(page);
				} else {
					// Up to date
				}
			}
		}
		db.close();

		return currentSyncMarker;
	}
}
