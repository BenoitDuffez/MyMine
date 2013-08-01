package net.bicou.redmine.app.wiki;

import android.app.Activity;
import android.content.Intent;
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
			}

			return super.shouldOverrideUrlLoading(view, url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			view.invalidate();
		}
	}

	public static String htmlFromTextile(final String textile) {
		if (textile == null) {
			return "";
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
		int firstCharPos, pos;
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

		return b.toString();
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
