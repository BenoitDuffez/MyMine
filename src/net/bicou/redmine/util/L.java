package net.bicou.redmine.util;

import android.util.Log;
import org.acra.ACRA;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class L {
	static final String TAG = "BicouRedmine";
	static long lastmsec = 0;
	static String log = "";

	public static void d(final String msg) {
		final String m = msg == null || msg.trim().length() == 0 ? "" : "\n" + msg;
		Log.d(TAG, logCurrentFunction(true) + m);
		logThis("D", logCurrentFunction(true) + m);
	}

	public static void e(final String msg) {
		final String m = msg;// == null || msg.trim().length() == 0 ? "" : "\n" + msg;
		Log.e(TAG, /* logCurrentFunction(true) + */m);
		logThis("E", logCurrentFunction(true) + m);
	}

	public static void e(final String msg, final Exception e) {
		e(msg);
		logException(e);
		ACRA.getErrorReporter().handleSilentException(e);
	}

	public static void i(final String msg) {
		Log.i(TAG, msg);
		logThis("I", msg);
	}

	public static void v(final String msg) {
		Log.v(TAG, msg);
		logThis("V", msg);
	}

	public static void w(final String msg) {
		Log.v(TAG, msg);
		logThis("W", msg);
	}

	private static void logThis(final String type, final String msg) {
		final Calendar now = new GregorianCalendar();
		int h, m, s;
		h = now.get(Calendar.HOUR);
		m = now.get(Calendar.MINUTE);
		s = now.get(Calendar.SECOND);

		log += type + "|";
		log += h < 10 ? "0" + h : h;
		log += ":" + (m < 10 ? "0" + m : m);
		log += ":" + (s < 10 ? "0" + s : s);
		log += "." + now.get(Calendar.MILLISECOND);

		log += "| " + msg + "\n";

		try {
			ACRA.getErrorReporter().removeCustomData("TraceLog");
			if (log.length() > 10000) {
				log = log.substring(10000 - log.length());
			}
			ACRA.getErrorReporter().putCustomData("TraceLog", log);
		} catch (final Exception e) {
		}
	}

	public static void time(final String msg) {
		final long current = new GregorianCalendar().getTimeInMillis();
		final long diff = current - lastmsec;
		lastmsec = current;
		String log = logCurrentFunction(false);

		if (lastmsec > 0) {
			log += "   delta = " + diff + " ms";
		}

		log += " -- " + current;

		if (msg != null && msg.length() > 0) {
			log += " (" + msg + ")";
		}

		Log.d(TAG, log);
	}

	public static void logException(final Exception e) {
		e("---------------------------------");
		e("Exception! " + (e == null ? "no exception" : e.toString()));
		e("---------------------------------");
		printStackTrace(e == null ? Thread.currentThread().getStackTrace() : e.getStackTrace());
		e("---------------------------------");
	}

	public static void logTrace() {
		printStackTrace(Thread.currentThread().getStackTrace());
	}

	private static void printStackTrace(final StackTraceElement[] trace) {
		for (final StackTraceElement element : trace) {
			e(element.getFileName() + ":" + element.getLineNumber() + " ==> " + element.getClassName() + "::" + element.getMethodName() + "()");
		}
	}

	public static String logCurrentFunction(final boolean isLong) {
		final StackTraceElement tr = Thread.currentThread().getStackTrace()[4];
		final String m = tr.getMethodName();
		final String f = tr.getFileName();
		final String l = "" + tr.getLineNumber();
		String c = tr.getClassName();
		c = c.substring(c.lastIndexOf(".") + 1);
		if (isLong) {
			return "--- " + f + ":" + l + " " + c + "::" + m + "() ---";
		}
		return "- " + c + "@" + l + ":" + m;
	}
}
