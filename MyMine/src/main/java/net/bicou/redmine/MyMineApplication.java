package net.bicou.redmine;

import android.app.Application;
import android.provider.Settings.Secure;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(mode = ReportingInteractionMode.SILENT, formKey = "", formUri = "http://crashes.bicou.net/submit.php", formUriBasicAuthLogin = "app",
		formUriBasicAuthPassword = "bd9628a9c1ec")
public class MyMineApplication extends Application {
	@Override
	public void onCreate() {
		ACRA.init(this);
		if ("ac3b9b819f6f48a6".equals(Secure.getString(getContentResolver(), Secure.ANDROID_ID))) {
			final ACRAConfiguration config = ACRA.getConfig();
			config.setFormUri("");
			ACRA.setConfig(config);
		}
		super.onCreate();

		// Create global configuration and initialize ImageLoader with this configuration
		final DisplayImageOptions options = new DisplayImageOptions.Builder() //
				.cacheOnDisc() //
				.build();
		final ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()) //
				.defaultDisplayImageOptions(options) //
				.build();
		ImageLoader.getInstance().init(config);
	}
}
