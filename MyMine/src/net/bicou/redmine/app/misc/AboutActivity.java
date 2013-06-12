package net.bicou.redmine.app.misc;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;
import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AboutActivity extends DrawerActivity {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		setupVersion();
		((WebView) findViewById(R.id.about_licenses)).loadUrl("file:///android_asset/about_licenses.html");
	}

	private void setupVersion() {
		// Get the build date
		long time = new Date().getTime();
		try {
			final ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
			final ZipFile zf = new ZipFile(ai.sourceDir);
			final ZipEntry ze = zf.getEntry("classes.dex");
			time = ze.getTime();
		} catch (final NameNotFoundException nnfe) {
			L.d("Unable to get last build date: " + nnfe);
		} catch (final IOException ioe) {
			L.d("Unable to get last build date: " + ioe);
		}
		final String buildDate = DateFormat.getInstance().format(new Date(time));

		// Get the build number
		String versionName = "MIN-153-406";
		try {
			final PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionName = String.format(getResources().getConfiguration().locale, "%s (r%d)", pi.versionName, pi.versionCode);
		} catch (final NameNotFoundException nnfe) {
			L.d("Unable to get last build date: " + nnfe);
		}

		final TextView version = (TextView) findViewById(R.id.about_version);
		version.setText(getString(R.string.about_version, versionName, buildDate));
	}
}
