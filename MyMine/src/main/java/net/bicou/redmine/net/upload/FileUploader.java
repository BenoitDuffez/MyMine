package net.bicou.redmine.net.upload;

import android.app.Activity;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.FileUpload;
import net.bicou.redmine.util.L;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * File upload class
 * Created by bicou on 02/04/2014.
 */
public class FileUploader extends JsonUploader {
	public static final String CONTENT_TYPE = "application/octet-stream";
	private File mFile;

	public Object uploadFile(Activity activity, Server server, File file) {
		mFile = file;
		if (mFile == null) {
			L.e("Shouldn't happen.", null);
			return null;
		}

		init(activity, server, "uploads.json");
		setStripJsonContainer(true);

		String json = downloadJson();
		if (json == null || getError() != null) {
			L.e("Unable to upload file", null);
		} else {
			return gsonParse(json, FileUpload.class);
		}

		return getError();
	}

	@Override
	protected HttpRequestBase getHttpRequest() {
		HttpPost post = new HttpPost(mURI);
		try {
			final AbstractHttpEntity entity = new InputStreamEntity(new FileInputStream(mFile), -1);
			entity.setContentType(CONTENT_TYPE);
			post.setHeader("Content-Type", CONTENT_TYPE);
			post.setEntity(entity);
			return post;
		} catch (FileNotFoundException e) {
			L.e("Unable to create a POST request for a file upload", e);
		}
		return null;
	}
}
