package net.bicou.redmine.app.misc;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.util.L;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AboutFragment extends TrackedFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_about, container, false);

		if (v != null) {
			final TextView version = (TextView) v.findViewById(R.id.about_version);
			setupVersion(version);

			String aboutLicenses = readTextFromResource(R.raw.about_licenses);
			((WebView) v.findViewById(R.id.about_licenses)).loadData(aboutLicenses, "text/html; charset=UTF-8", "UTF-8");
		}

		return v;
	}

	private String readTextFromResource(int resourceID) {
		InputStream raw = getResources().openRawResource(resourceID);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		int i;
		try {
			i = raw.read();
			while (i != -1) {
				stream.write(i);
				i = raw.read();
			}
			raw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stream.toString();
	}

	private void setupVersion(TextView version) {
		PackageManager pm = getActivity().getPackageManager();
		String packageName = getActivity().getPackageName();

		if (pm == null) {
			L.e("Unable to load PM", null);
			return;
		}

		// Get the build date
		long time = new Date().getTime();
		try {
			final ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
			final ZipFile zf = new ZipFile(ai.sourceDir);
			final ZipEntry ze = zf.getEntry("classes.dex");
			if (ze != null) {
				time = ze.getTime();
			}
		} catch (final NameNotFoundException nnfe) {
			L.e("Unable to get last build date: ", nnfe);
		} catch (final IOException ioe) {
			L.e("Unable to get last build date: ", ioe);
		}
		final String buildDate = DateFormat.getInstance().format(new Date(time));

		// Get the build number
		String versionName = "MIN-153-406";
		try {
			final PackageInfo pi = pm.getPackageInfo(packageName, 0);
			versionName = String.format(getResources().getConfiguration().locale, "%s (r%d)", pi.versionName, pi.versionCode);
		} catch (final NameNotFoundException nnfe) {
			L.d("Unable to get last build date: " + nnfe);
		} catch (NullPointerException npe) {
			L.d("Unable to get last build date: " + npe);
		}

		version.setText(getString(R.string.about_version, versionName, buildDate));
	}
}
