package net.bicou.redmine;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MyMineApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Crashlytics.start(this);

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
