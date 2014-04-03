package net.bicou.redmine.data.json;

/**
 * Simple container for the server response upon a file upload.
 * Created by bicou on 02/04/2014.
 */
public class FileUpload {
	public static final String EXTRA_TOKEN = "net.bicou.redmine.data.json.FileUpload.token";
	public static final String EXTRA_FILENAME = "net.bicou.redmine.data.json.FileUpload.filename";

	public String token;
	public String filename;
	public String description; // TODO
	public String content_type; // TODO

	@Override
	public String toString() {
		return getClass().getSimpleName() + " { token: " + token + " }";
	}
}
