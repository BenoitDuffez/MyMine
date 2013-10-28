package net.bicou.redmine.data.json;

import java.util.List;

/**
 * Created by bicou on 28/10/2013.
 * Matches the errors list provided by the server when it replies with {@link org.apache.http.HttpStatus#SC_UNPROCESSABLE_ENTITY HTTP 422}
 */
public class ErrorsList {
	public List<String> errors;
}
