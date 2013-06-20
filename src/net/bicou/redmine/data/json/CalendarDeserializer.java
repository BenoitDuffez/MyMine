package net.bicou.redmine.data.json;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import net.bicou.redmine.util.L;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class CalendarDeserializer implements JsonDeserializer<Calendar> {
	private static final String[] formats = new String[] {
			"yyyy-MM-d'T'HH:mm:ssZ",
			"yyyy/MM/d HH:mm:ss Z",
			"yyyy/MM/d",
			"yyyy-MM-d",
	};

	@Override
	public Calendar deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
		final Calendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(0);

		String date = json.getAsString();
		if (date.charAt(date.length() - 1) == 'Z') {
			date = date.substring(0, date.length() - 1) + "+0000";
		}

		boolean ok = false;
		for (final String format : formats) {
			final SimpleDateFormat parser = new SimpleDateFormat(format, Locale.ENGLISH);
			try {
				cal.setTime(parser.parse(date));
				ok = true;
				break;
			} catch (final ParseException e) {
			}
		}
		if (ok) {
			return cal;
		}
		L.e("unable to parse '" + json.getAsString() + "'!");
		return null;
	}
}
