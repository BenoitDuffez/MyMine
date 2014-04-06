package net.bicou.redmine.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import net.bicou.redmine.R;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Util {
	// http://code.google.com/p/android/issues/detail?id=58108
	public static int getContentViewCompat() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ? android.R.id.content : android.support.v7.appcompat.R.id.action_bar_activity_content;
	}

	public static String getDeltaDateText(Context context, Calendar date) {
		final String formattedDate;
		if (date != null && date.getTimeInMillis() > 10000) {
			long delta = (new GregorianCalendar().getTimeInMillis() - date.getTimeInMillis()) / 1000;
			if (delta < 60) {
				formattedDate = context.getString(R.string.time_delay_moments);
			} else if (delta < 3600) {
				formattedDate = MessageFormat.format(context.getString(R.string.time_delay_minutes), (int) (delta / 60));
			} else if (delta < 3600 * 24) {
				formattedDate = MessageFormat.format(context.getString(R.string.time_delay_hours), (int) (delta / 3600));
			} else if (delta < 3600 * 24 * 30) {
				formattedDate = MessageFormat.format(context.getString(R.string.time_delay_days), (int) (delta / (3600 * 24)));
			} else if (delta < 3600 * 24 * 365) {
				formattedDate = MessageFormat.format(context.getString(R.string.time_delay_months), (int) (delta / (3600 * 24 * 30)));
			} else {
				formattedDate = MessageFormat.format(context.getString(R.string.time_delay_years), (int) (delta / (3600 * 24 * 365)));
			}
		} else {
			formattedDate = "";
		}

		return formattedDate;
	}

	public static String readableFileSize(long size) {
		if (size <= 0) { return "0 B"; }
		// TODO: localize these?
		final String[] units = new String[] {
				"B",
				"kiB",
				"MiB",
				"GiB",
				"TiB"
		};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	public static boolean isEpoch(Calendar cal) {
		return cal == null || cal.getTimeInMillis() < 24 * 3600 * 1000;
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
