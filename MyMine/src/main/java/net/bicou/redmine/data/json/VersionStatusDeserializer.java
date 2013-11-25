package net.bicou.redmine.data.json;

import java.lang.reflect.Type;

import net.bicou.redmine.data.json.Version.VersionStatus;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class VersionStatusDeserializer implements JsonDeserializer<VersionStatus> {
	@Override
	public VersionStatus deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
		final VersionStatus status;

		final String s = json.getAsString();
		if ("open".equals(s)) {
			status = VersionStatus.OPEN;
		} else if ("locked".equals(s)) {
			status = VersionStatus.LOCKED;
		} else if ("closed".equals(s)) {
			status = VersionStatus.CLOSED;
		} else {
			status = VersionStatus.INVALID;
		}

		return status;
	}
}
