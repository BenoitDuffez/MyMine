package net.bicou.redmine.net.upload;

import android.app.Activity;
import android.view.ViewGroup;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.FileUpload;
import net.bicou.redmine.util.L;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.FileEntity;

import java.io.File;

/**
 * File upload class
 * Created by bicou on 02/04/2014.
 */
public class FileUploader extends JsonUploader {
	public static final String CONTENT_TYPE = "binary/octet-stream";
	private File mFile;
	int mViewGroupResId = 0;
	ViewGroup mViewGroup = null;

	public FileUploader(int errorCroutonViewHolderResId) {
		mViewGroupResId = errorCroutonViewHolderResId;
	}

	public FileUploader(ViewGroup errorCroutonViewHolder) {
		mViewGroup = errorCroutonViewHolder;
	}

	public FileUpload uploadFile(Activity activity, Server server, File file) {
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
			if (mViewGroupResId > 0) {
				getError().displayCrouton(activity, mViewGroupResId);
			} else {
				getError().displayCrouton(activity, mViewGroup);
			}
		} else {
			return gsonParse(json, FileUpload.class);
		}

		return null;
	}

	@Override
	protected HttpRequestBase getHttpRequest() {
		HttpPost post = new HttpPost(mURI);
		FileEntity entity;
		entity = new FileEntity(mFile, CONTENT_TYPE);
		entity.setContentEncoding("UTF-8");
		entity.setContentType(CONTENT_TYPE);
		post.setEntity(entity);
		return post;
	}
}
