package net.bicou.redmine.app.wiki;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.bicou.redmine.Constants;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.util.L;
import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.textile.TextileDialect;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bicou on 30/07/13.
 */
public class WikiUtils {
	private static final Object SPECIAL_LINK_SEPARATOR = "=";
	public static final String SPECIAL_LINK_OPERATOR_PATTERN = "^([a-z]+)" + SPECIAL_LINK_SEPARATOR + "(.+)$";

	public static class WikiWebViewClient extends WebViewClient {
		Project mProject;
		Activity mActivity;

		public WikiWebViewClient(Activity activity) {
			mActivity = activity;
		}

		public void setProject(Project project) {
			mProject = project;
		}

		@Override
		public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
			Pattern specialLink = Pattern.compile(SPECIAL_LINK_OPERATOR_PATTERN);
			Matcher match = specialLink.matcher(url);
			if (match.find()) {
				String operator = match.group(1);
				if ("wiki".equals(operator)) {
					final Bundle args = new Bundle();
					String uri = WikiPageLoader.getUriFromTitle(match.group(2));
					args.putString(WikiPageFragment.KEY_WIKI_PAGE_URI, uri);
					args.putLong(Constants.KEY_PROJECT_ID, mProject.id);
					args.putLong(Constants.KEY_SERVER_ID, mProject.server.rowId);
					if (mActivity instanceof WikiActivity) {
						((WikiActivity) mActivity).selectContent(args);
					} else {
						Intent intent = new Intent(mActivity, WikiActivity.class);
						intent.putExtras(args);
						mActivity.startActivity(intent);
					}
					return true;
				} else if ("issue".equals(operator)) {
					long issueId = 0;
					try {
						issueId = Long.parseLong(match.group(2));
					} catch (NumberFormatException e) {
						issueId = 0;
					}
					if (issueId > 0) {
						Bundle args = new Bundle();
						args.putLong(Constants.KEY_ISSUE_ID, issueId);
						args.putLong(Constants.KEY_SERVER_ID, mProject.server.rowId);
						if (mActivity instanceof IssuesActivity) {
							((IssuesActivity) mActivity).selectContent(args);
						} else {
							Intent intent = new Intent(mActivity, IssuesActivity.class);
							intent.putExtras(args);
							mActivity.startActivity(intent);
						}
					}
				} else {
					L.e("Unhandled wiki link: " + url);
					return super.shouldOverrideUrlLoading(view, url);
				}

				return true;
			} else if (view != null && view.getContext() != null) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				view.getContext().startActivity(i);

				return true;
			}

			return super.shouldOverrideUrlLoading(view, url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			view.invalidate();
		}
	}

	public static String htmlFromTextile(String textile) {
		if (textile == null) {
			return "";
		}

		// Table of contents
		String headerPattern = "[hH]([0-4])\\. ([^\r\n]+)";
		int pos = textile.indexOf("{{toc}}");
		if (pos >= 0) {
			String toc = "<style type=\"text/css\">" +
					"div#toc { background: #ffe; border-color: #333; padding: 1em; margin-left: 2em; display: inline-block; }" +
					"div#toc ol { padding: 0em 1em; margin: 0em; }" +
					"</style>" +
					"<div id=\"toc\">";
			Pattern h = Pattern.compile(headerPattern);
			Matcher headingsMatcher = h.matcher(textile);
			int prevLevel = 0, level;
			String title;
			while (headingsMatcher.find()) {
				L.i(headingsMatcher.group());
				level = Integer.parseInt(headingsMatcher.group(1));
				if (level > prevLevel) {
					for (int i = prevLevel; i < level; i++) {
						toc += "<ol>";
					}
				} else if (level < prevLevel) {
					for (int i = level; i < prevLevel; i++) {
						toc += "</ol>";
					}
				}

				// Remove link if there is one
				title = headingsMatcher.group(2);
				if (title.matches("^\\[\\[.+?\\]\\]$")) {
					title = title.substring(2, title.length() - 2);
				}

				// Place bullet and item
				String anchor = title.replaceAll("[^a-zA-Z0-9_]", "");
				toc += "<li><a href=\"#" + anchor + "\">" + title + "</a></li>";
				prevLevel = level;
			}
			while (prevLevel > 0) {
				toc += "</ol>";
				prevLevel--;
			}
			toc += "</div>";

			textile = textile.replace("{{toc}}", toc);
		}

		final MarkupParser parser = new MarkupParser(new TextileDialect());
		final StringWriter sw = new StringWriter();
		final HtmlDocumentBuilder builder = new HtmlDocumentBuilder(sw);
		builder.setEmitAsDocument(false);
		parser.setBuilder(builder);
		parser.parse(textile.trim());

		// Bring back html entities
		String html = sw.toString();
		html = html.replace("&amp;nbsp;", "&nbsp;");

		// [[linkTarget|Link name]]
		html = html.replaceAll("\\[\\[([^\\|]+)\\|([^\\]]+)\\]\\]", "<a href=\"wiki" + SPECIAL_LINK_SEPARATOR + "$1\">$2</a>");

		// [[link]]
		html = html.replaceAll("\\[\\[([^\\]]+)\\]\\]", "<a href=\"wiki" + SPECIAL_LINK_SEPARATOR + "$1\">$1</a>");

		// Issue
		html = html.replaceAll("#([0-9]+)([^;0-9]|\\z)", "<a href=\"issue" + SPECIAL_LINK_SEPARATOR + "$1\">#$1</a>$2");

		// Turn on block quotes by >
		final String[] lines = html.replace("<br/>", "<br/>\n").replace("</p>", "</p>\n").split("\n");
		html = "";
		final StringBuilder b = new StringBuilder();
		int blockQuoteLevel = 0;
		int firstCharPos;
		char c;
		for (final String line : lines) {
			if (line.length() > 0) {
				firstCharPos = 0;

				if (line.charAt(0) == '<') {
					while (firstCharPos < line.length() && line.charAt(firstCharPos) != '>') {
						firstCharPos++;
					}
					firstCharPos++;
				}

				if (firstCharPos < line.length() && line.charAt(firstCharPos) == '>') {
					int newBlockQuoteLevel = 1;
					pos = firstCharPos + 1;
					while (pos < line.length()) {
						c = line.charAt(pos++);
						firstCharPos++;

						if (c == '>') {
							newBlockQuoteLevel++;
						} else if (c == ' ' || c == '\t') {
							continue;
						} else {
							break;
						}
					}

					if (newBlockQuoteLevel > blockQuoteLevel) {
						for (int i = newBlockQuoteLevel; i > blockQuoteLevel; i--) {
							b.append("<blockquote>");
						}
					} else {
						for (int i = blockQuoteLevel; i > newBlockQuoteLevel; i--) {
							b.append("</blockquote>");
						}
					}
					blockQuoteLevel = newBlockQuoteLevel;
					b.append(line.substring(firstCharPos));
				} else {
					while (blockQuoteLevel > 0) {
						b.append("</blockquote>");
						blockQuoteLevel--;
					}
					b.append(line);
				}
			}
		}

		// http://daringfireball.net/2010/07/improved_regex_for_matching_urls
		String output = b.toString();
		output = output.replaceAll("(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2," +
				"4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".," +
				"<>?«»“”‘’]))", "<a href=\"$1\">$1</a>");

		return output;
	}

	public static String titleFromUri(String uri) {
		String wikiPageTitle = null;
		try {
			wikiPageTitle = URLDecoder.decode(uri, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			L.e("Unable to decode wiki page title: " + uri, e);
		}
		return wikiPageTitle;
	}
}
