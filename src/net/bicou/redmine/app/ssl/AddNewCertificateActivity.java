package net.bicou.redmine.app.ssl;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.ipaulpro.afilechooser.utils.FileUtils;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.R;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class AddNewCertificateActivity extends SherlockFragmentActivity {
	Uri mFileUri;
	TextView mFileUriInfo;
	ViewGroup mCroutonHolder;

	public static final String KEY_FILE_URI = "net.bicou.redmine.app.ssl.CertFileUri";
	private static final int REQUEST_CODE = 1234;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_certificate);

		if (savedInstanceState != null) {
			final String uri = savedInstanceState.getString(KEY_FILE_URI);
			if (!TextUtils.isEmpty(uri)) {
				mFileUri = Uri.parse(uri);
			}
		}

		mFileUriInfo = (TextView) findViewById(R.id.add_cert_settings_cert_info);
		mCroutonHolder = (ViewGroup) findViewById(R.id.server_auth_layout);

		findViewById(R.id.add_cert_select_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View clicked) {
				final Intent target = FileUtils.createGetContentIntent();
				final Intent intent = Intent.createChooser(target, getString(R.string.server_auth_settings_select_cert));
				try {
					startActivityForResult(intent, REQUEST_CODE);
				} catch (final ActivityNotFoundException e) {
					// The reason for the existence of aFileChooser
				}
			}
		});

		findViewById(R.id.add_cert_ok_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mFileUri != null) {
					final X509Certificate cert = getCert(R.string.server_auth_cert_save_ok);
					if (cert != null) {
						final KeyStoreDiskStorage ks = new KeyStoreDiskStorage(AddNewCertificateActivity.this);
						final X509Certificate[] chain = new X509Certificate[] {
								cert
						};
						ks.storeCert(chain);
					}
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	private X509Certificate getCert(final int successTextResId) {
		if (mFileUri == null || TextUtils.isEmpty(mFileUri.toString())) {
			return null;
		}
		X509Certificate cert = null;
		final File file = FileUtils.getFile(mFileUri);

		try {
			final CertificateFactory factory = CertificateFactory.getInstance("X.509");
			final InputStream is = new FileInputStream(file);
			cert = (X509Certificate) factory.generateCertificate(is);

			Crouton.makeText(this, successTextResId, Style.CONFIRM, mCroutonHolder).show();
		} catch (final CertificateException e) {
			Crouton.makeText(this, R.string.server_auth_cert_cert_exception, Style.ALERT, mCroutonHolder).show();
		} catch (final FileNotFoundException e) {
			Crouton.makeText(this, R.string.server_auth_cert_file_exception, Style.ALERT, mCroutonHolder).show();
		}

		return cert;
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case REQUEST_CODE:
			if (resultCode == Activity.RESULT_OK) {
				mFileUri = data == null ? null : data.getData();
				if (mFileUri != null) {
					mFileUriInfo.setText(mFileUri.getLastPathSegment());
				}

				getCert(R.string.server_auth_cert_check_ok);
			} else {
				mFileUriInfo.setText(R.string.server_auth_settings_no_cert);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_FILE_URI, mFileUri == null ? null : mFileUri.toString());
	}
}
