package net.bicou.redmine.util;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;

public class StrikeTagHandler implements TagHandler {
	@Override
	public void handleTag(final boolean opening, final String tag, final Editable output, final XMLReader xmlReader) {
		if (tag.equalsIgnoreCase("strike") || tag.equals("s")) {
			processStrike(opening, output);
		}
	}

	private void processStrike(final boolean opening, final Editable output) {
		final int len = output.length();
		if (opening) {
			output.setSpan(new StrikethroughSpan(), len, len, Spanned.SPAN_MARK_MARK);
		} else {
			final Object obj = getLast(output, StrikethroughSpan.class);
			final int where = output.getSpanStart(obj);

			output.removeSpan(obj);

			if (where != len) {
				output.setSpan(new StrikethroughSpan(), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

	private Object getLast(final Editable text, final Class<?> kind) {
		final Object[] objs = text.getSpans(0, text.length(), kind);

		if (objs.length == 0) {
			return null;
		} else {
			for (int i = objs.length; i > 0; i--) {
				if (text.getSpanFlags(objs[i - 1]) == Spanned.SPAN_MARK_MARK) {
					return objs[i - 1];
				}
			}
			return null;
		}
	}
}
