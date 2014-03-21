package net.bicou.redmine.app.misc;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.vending.billing.IInAppBillingService;

/**
 * Used to provide the users a mean to donate
 * Created by bicou on 18/03/2014.
 */
public class DonateFragment extends Fragment {
	IInAppBillingService mService;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
}
