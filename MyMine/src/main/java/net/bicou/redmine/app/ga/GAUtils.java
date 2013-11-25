package net.bicou.redmine.app.ga;

import android.support.v4.app.Fragment;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

/**
 * Created by bicou on 07/10/13.
 */
public class GAUtils {
	public static void trackPageView(Fragment fragment) {
		String screenName = fragment.getActivity().getClass().getName() + "/" + fragment.getClass().getName();
		EasyTracker.getInstance(fragment.getActivity()).send(MapBuilder.createAppView().set(Fields.SCREEN_NAME, screenName).build());
	}

	public static void trackPageView(android.app.Fragment fragment) {
		String screenName = fragment.getActivity().getClass().getName() + "/" + fragment.getClass().getName();
		EasyTracker.getInstance(fragment.getActivity()).send(MapBuilder.createAppView().set(Fields.SCREEN_NAME, screenName).build());
	}
}
