package net.bicou.redmine.app.ssl;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import com.google.analytics.tracking.android.EasyTracker;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.misc.EmptyFragment;
import net.bicou.redmine.net.ssl.MyMineSSLKeyManager;
import net.bicou.redmine.util.PreferencesManager;
import net.bicou.splitactivity.SplitActivity;

public class KeyStoreManagerActivity extends SplitActivity<CertificatesListFragment, CertificateFragment> implements AsyncTaskFragment.TaskFragmentCallbacks {
	@Override
	protected Fragment createEmptyFragment(Bundle args) {
		return EmptyFragment.newInstance(R.drawable.empty_cert);
	}

	@Override
	protected CertificatesListFragment createMainFragment(Bundle args) {
		return CertificatesListFragment.newInstance(args);
	}

	@Override
	protected CertificateFragment createContentFragment(Bundle args) {
		return CertificateFragment.newInstance(args);
	}

    @Override
    protected void onPreCreate() {
        supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    }

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);

		AsyncTaskFragment.attachAsyncTaskFragment(this);
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

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_certificates, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_certificate_add:
			startActivity(new Intent(this, AddNewCertificateActivity.class));
			return true;

		case R.id.menu_certificate_import:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				importCertificate();
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveAlias(final String alias) {
		PreferencesManager.setString(this, MyMineSSLKeyManager.KEY_CERTIFICATE_ALIAS, alias);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final CertificatesListFragment f = getMainFragment();
				if (f != null) {
					f.refreshList();
				}
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void importCertificate() {
		KeyChain.choosePrivateKeyAlias(this, new KeyChainAliasCallback() {
			@Override
			public void alias(final String alias) {
				// Credential alias selected. Remember the alias selection for future use.
				if (alias != null) {
					saveAlias(alias);
				}
			}
		}, new String[] {
				"RSA",
				"DSA"
		}, // List of acceptable key types. null for any
				null, // issuer, null for any
				null,// "internal.example.com", // host name of server requesting the cert, null if unavailable
				-1,// 443, // port of server requesting the cert, -1 if unavailable
				null); // alias to preselect, null if unavailable
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(Context applicationContext, final int action, final Object parameters) {
		if (getContentFragment() != null) {
			getContentFragment().loadCertificate((String) parameters);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		if (getContentFragment() != null) {
			getContentFragment().refreshUI();
		}
	}
}
