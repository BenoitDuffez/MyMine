package net.bicou.redmine.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import com.google.gson.Gson;
import net.bicou.redmine.app.issues.IssuesOrderColumnsAdapter;
import net.bicou.redmine.app.issues.IssuesOrderingFragment;
import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.textile.TextileDialect;

import java.io.StringWriter;
import java.util.ArrayList;

public class Util {
	public static ArrayList<IssuesOrderColumnsAdapter.OrderColumn> getPreferredIssuesOrder(Context ctx) {
		ArrayList<IssuesOrderColumnsAdapter.OrderColumn> mCurrentOrder = null;

		final String json = PreferencesManager.getString(ctx, IssuesOrderingFragment.KEY_COLUMNS_ORDER, null);
		if (!TextUtils.isEmpty(json)) {
			try {
				mCurrentOrder = new Gson().fromJson(json, IssuesOrderingFragment.ORDER_TYPE);
			} catch (final Exception e) {
				PreferencesManager.setString(ctx, IssuesOrderingFragment.KEY_COLUMNS_ORDER, null);
			}
		}
		if (mCurrentOrder == null) {
			mCurrentOrder = IssuesOrderColumnsAdapter.OrderColumn.getDefaultOrder();
		}

		return mCurrentOrder;
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

				if (line.charAt(firstCharPos) == '>') {
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

	public static String joinFirstWords(final Object[] values, final String delim) {
		final Object[] firstWords = new Object[values.length];
		int i = 0;
		for (final Object o : values) {
			final String word = o.toString();
			final int pos = word.indexOf(" ");
			if (pos > 0) {
				firstWords[i] = word.substring(0, pos);
			} else {
				firstWords[i] = word;
			}
			i++;
		}
		return join(firstWords, delim);
	}

	public static String join(final Object[] values, final String delim) {
		if (values == null) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();

		int i;
		final int n = values.length;
		for (i = 0; i < n; i++) {
			if (i > 0 && delim != null) {
				sb.append(delim);
			}
			if (values[i] != null) {
				sb.append(values[i]);
			} else {
				sb.append("");
			}
		}

		return sb.toString();
	}

	@SuppressWarnings("deprecation")
	private static int getSupportSmallestScreenWidthDp(final Context ctx) {
		final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		final Display d = wm.getDefaultDisplay();
		int width, height, smallestWidthPx, smallestWidthDp;
		width = d.getWidth();
		height = d.getHeight();

		smallestWidthPx = width > height ? height : width;
		smallestWidthDp = (int) (smallestWidthPx / ctx.getResources().getDisplayMetrics().density);

		return smallestWidthDp;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private static int getSmallestScreenWidthDp(final Configuration c) {
		return c.smallestScreenWidthDp;
	}

	public static int getSmallestScreenWidthDp(final Context ctx) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			return getSupportSmallestScreenWidthDp(ctx);
		} else {
			return getSmallestScreenWidthDp(ctx.getResources().getConfiguration());
		}
	}
}
