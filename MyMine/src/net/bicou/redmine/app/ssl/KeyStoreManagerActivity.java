package net.bicou.redmine.app.ssl;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.v4.app.Fragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.redmine.R;
import net.bicou.redmine.net.ssl.MyMineSSLKeyManager;
import net.bicou.redmine.util.PreferencesManager;

public class KeyStoreManagerActivity extends SherlockFragmentActivity {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_certificates);

		final Bundle args = new Bundle();

		// Setup fragments
		if (savedInstanceState == null) {
			// Setup list view
			getSupportFragmentManager().beginTransaction().replace(R.id.certificates_pane_list, CertificatesListFragment.newInstance(args)).commit();
		} else if (savedInstanceState.containsKey(CertificateFragment.KEY_CERT_ALIAS)) {
			args.putString(CertificateFragment.KEY_CERT_ALIAS, savedInstanceState.getString(CertificateFragment.KEY_CERT_ALIAS));
			// Setup content view, if possible
			//TODO
			//if (mIsSplitScreen) {
			//	getSupportFragmentManager().beginTransaction().replace(R.id.certificates_pane_certificate, CertificateFragment.newInstance(args)).commit();
			//}
		}

		// Screen rotation on 7" tablets
		// TODO
		//if (savedInstanceState != null && mIsSplitScreen != savedInstanceState.getBoolean(KEY_IS_SPLIT_SCREEN)) {
		//		final Fragment f = getSupportFragmentManager().findFragmentById(R.id.certificates_pane_list);
		//		if (f != null && f instanceof CertificatesListFragment) {
		//			((CertificatesListFragment) f).updateSplitScreenState(mIsSplitScreen);
		//		}
		//	}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getSupportMenuInflater();
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
				final Fragment f = getSupportFragmentManager().findFragmentById(R.id.certificates_pane_list);
				if (f != null && f instanceof CertificatesListFragment) {
					((CertificatesListFragment) f).refreshList();
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
}
