package net.bicou.redmine.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import java.util.Calendar;

public class Util {
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
