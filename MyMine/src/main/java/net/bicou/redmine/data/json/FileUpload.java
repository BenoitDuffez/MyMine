package net.bicou.redmine.data.json;

/**
 * Simple container for the server response upon a file upload.
 * Created by bicou on 02/04/2014.
 */
public class FileUpload {
	String token;

	@Override
	public String toString() {
		return getClass().getSimpleName() + " { token: " + token + " }";
	}
}
