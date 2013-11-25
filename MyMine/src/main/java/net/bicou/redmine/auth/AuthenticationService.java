package net.bicou.redmine.auth;

import net.bicou.redmine.util.L;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle Account authentication. It instantiates the authenticator and returns its IBinder.
 */
public class AuthenticationService extends Service {
	private Authenticator mAuthenticator;

	@Override
	public void onCreate() {
		L.v("SampleSyncAdapter Authentication Service started.");
		mAuthenticator = new Authenticator(this);
	}

	@Override
	public void onDestroy() {
		L.v("SampleSyncAdapter Authentication Service stopped.");
	}

	@Override
	public IBinder onBind(final Intent intent) {
		L.v("getBinder()...  returning the AccountAuthenticator binder for intent " + intent);
		return mAuthenticator.getIBinder();
	}
}
