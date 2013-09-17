package net.bicou.redmine.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.bicou.redmine.R;

/**
 * Pseudo-action bar that displays a cancel/save action bar.
 * TODO: should be absolutely deleted and replaced with a proper implementation
 * Created by bicou on 06/08/13.
 */
public class CancelSaveActionBar {
	public interface CancelSaveActionBarCallbacks {
		public void onSave();
	}

	public static void setupActionBar(final SherlockFragmentActivity activity, final CancelSaveActionBarCallbacks callbacks) {
		final View actionView = LayoutInflater.from(activity).inflate(R.layout.actionbar_cancel_save, null);
		assert actionView != null;

		View.OnClickListener ocl = new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				switch (v.getId()) {
				case R.id.actionbar_button_save:
					if (callbacks != null) {
						callbacks.onSave();
					}
					break;

				case R.id.actionbar_button_cancel:
					break;
				}
				// Don't do anything if the button has been clicked, it's up to the client activity to disable this weird action bar
				v.setOnClickListener(null);
			}
		};

		actionView.findViewById(R.id.actionbar_button_save).setOnClickListener(ocl);
		actionView.findViewById(R.id.actionbar_button_cancel).setOnClickListener(ocl);

		ActionBar actionBar = activity.getSupportActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setCustomView(actionView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		actionBar.setDisplayShowCustomEnabled(true);
	}
}
