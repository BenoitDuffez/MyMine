package net.bicou.redmine.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.auth.AdvancedServerSettingsFragment.ServerSettingsListener;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.net.JsonDownloadError;
import net.bicou.redmine.net.JsonDownloadError.ErrorType;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.sync.SyncUtils;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.security.cert.X509Certificate;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorSherlockActivity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
		ServerSettingsListener, AsyncTaskFragment.TaskFragmentCallbacks {
	public static final String PARAM_SERVER_URL = "serverUrl";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	private AccountManager mAccountManager;
	protected boolean mRequestNewAccount = false;

	private EditText mApiKeyEdit;

	private EditText mServerUrlEdit;

	KeyStoreDiskStorage mKeyStoreDisk;

	private static final String KEY_SERVER_URL = "net.bicou.redmine.auth.ServerURL";
	private static final String KEY_SERVER_API_KEY = "net.bicou.redmine.auth.ServerAPIKey";
	private static final String KEY_AUTH_USERNAME = "net.bicou.redmine.auth.HttpUserName";
	private static final String KEY_AUTH_PASSWORD = "net.bicou.redmine.auth.HttpUserPassword";

	public boolean mIsAuthSettingsFragmentShown = false;
	private String mAuthUsername, mAuthPassword;
	private ViewGroup mCroutonHolder;
	private X509Certificate[] mUntrustedCertChain;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setProgressBarIndeterminateVisibility(false);
		AsyncTaskFragment.attachAsyncTaskFragment(this);

		mAccountManager = AccountManager.get(this);
		mKeyStoreDisk = new KeyStoreDiskStorage(AuthenticatorActivity.this);

		// Load data from intent
		final Intent intent = getIntent();
		mRequestNewAccount = intent.getStringExtra(PARAM_SERVER_URL) == null;
		L.i("    request new: " + mRequestNewAccount);

		// Handle layout and views
		setContentView(R.layout.activity_server);
		mCroutonHolder = (ViewGroup) findViewById(R.id.setup_crouton_holder);
		mServerUrlEdit = (EditText) findViewById(R.id.setup_server_url);
		mApiKeyEdit = (EditText) findViewById(R.id.setup_api_key);
		mServerUrlEdit.setText(intent.getStringExtra(PARAM_SERVER_URL));

		// Restore state
		if (savedInstanceState != null) {
			final String url = savedInstanceState.getString(KEY_SERVER_URL);
			if (!TextUtils.isEmpty(url)) {
				mServerUrlEdit.setText(url);
			}
			final String apiKey = savedInstanceState.getString(KEY_SERVER_API_KEY);
			if (!TextUtils.isEmpty(apiKey)) {
				mApiKeyEdit.setText(apiKey);
			}
			mAuthUsername = savedInstanceState.getString(KEY_AUTH_USERNAME);
			mAuthPassword = savedInstanceState.getString(KEY_AUTH_PASSWORD);
		}

		// Handle help link
		findViewById(R.id.setup_apikey_help).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				String serverUrl = mServerUrlEdit.getText().toString();
				if (serverUrl != null && !serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
					serverUrl = "http://" + serverUrl;
				}
				if (!TextUtils.isEmpty(serverUrl) && Uri.parse(serverUrl).isAbsolute()) {
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(serverUrl + "/my/account"));
					try {
						startActivity(intent);
					} catch (final Exception e) {
						Toast.makeText(AuthenticatorActivity.this, getString(R.string.setup_apikey_help_toast), Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(AuthenticatorActivity.this, getString(R.string.setup_apikey_help_toast), Toast.LENGTH_LONG).show();
				}
			}
		});
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
	public void onCredentialsEntered(final String username, final String password) {
		mAuthUsername = username;
		mAuthPassword = password;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SERVER_URL, mServerUrlEdit.getText().toString());
		outState.putString(KEY_SERVER_API_KEY, mApiKeyEdit.getText().toString());
		outState.putString(KEY_AUTH_USERNAME, mAuthUsername);
		outState.putString(KEY_AUTH_PASSWORD, mAuthPassword);
	}

	/**
	 * Handle the onClick event on the Submit button. Send serverUrl/apiKey to the server for authentication.
	 *
	 * @param view The Submit button for which this method is invoked
	 */
	public void checkServer(final View view) {
		String serverUrl;
		if (mRequestNewAccount) {
			serverUrl = mServerUrlEdit.getText() == null ? null : mServerUrlEdit.getText().toString();
		} else {
			serverUrl = "";
		}
		String apiKey = mApiKeyEdit.getText() == null ? null : mApiKeyEdit.getText().toString();

		UserLoginTask.UserLoginParameters params = new UserLoginTask.UserLoginParameters(serverUrl, apiKey, mAuthUsername, mAuthPassword);
		AsyncTaskFragment.runTask(this, 0, params);
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		L.d("");
		setSupportProgressBarVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		L.d("");
		return UserLoginTask.tryUserLogin(applicationContext, (UserLoginTask.UserLoginParameters) parameters);
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		L.d("");
		setSupportProgressBarVisibility(false);
		if (result == null) {
			onLoginFailed(null);
		} else {
			if (result instanceof UserLoginTask.UserLoginResult) {
				final UserLoginTask.UserLoginResult loginResult = (UserLoginTask.UserLoginResult) result;
				if (loginResult.success()) {
					onLoginSuccessful(loginResult.server);
				} else {
					onLoginFailed((JsonDownloadError) loginResult.error);
				}
			} else {
				onLoginFailed(null);
			}
		}
	}

	/**
	 * Called when response is received from the server for authentication request. See onAuthenticationResult(). Sets the AccountAuthenticatorResult which is sent
	 * back
	 * to the caller. We store the authToken that's returned from the server as the 'apiKey' for this account - so we're never storing the user's actual apiKey
	 * locally.
	 */
	private void onLoginSuccessful(Server server) {
		L.d("");
		// Create account
		final Account account = new Account(server.serverUrl, Constants.ACCOUNT_TYPE);
		if (mRequestNewAccount) {
			mAccountManager.addAccountExplicitly(account, server.apiKey, null);
		} else {
			mAccountManager.setPassword(account, server.apiKey);
		}

		// Save data
		UserLoginTask.saveServer(this, server);
		SyncUtils.enableSync(account, this); // TODO: slow

		// Validate account creation
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, server.serverUrl);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	private void onLoginFailed(JsonDownloadError error) {
		L.e("onAuthenticationResult: failed to authenticate");
		if (mRequestNewAccount) {
			final String errorMessage;

			switch (error.errorType) {
			case TYPE_NETWORK:
				final String details;
				if (error.chain != null) {
					mUntrustedCertChain = error.chain;
					showSSLCertDialog();
					errorMessage = null;//don't show any crouton
				} else {
					if (error.exception == null) {
						details = error.getMessage(this);
					} else {
						details = error.exception.getClass().getSimpleName() + (error.exception.getMessage() == null ? //
								error.exception.getCause().toString() : error.exception.getMessage());
					}
					errorMessage = getString(error.errorType == ErrorType.TYPE_JSON ? R.string.auth_error_json : R.string.auth_error_network, details);
				}
				break;
			default:
			case TYPE_RESPONSE:
			case TYPE_UNKNOWN:
				errorMessage = getString(R.string.auth_error_response);
				break;
			case TYPE_JSON:
				errorMessage = getString(R.string.auth_error_json);
				break;
			}

			if (errorMessage != null) {
				Crouton.makeText(this, errorMessage + "\n" + getString(R.string.setup_button_tryagain), Style.ALERT, mCroutonHolder).show();
			}
		} else {
			Crouton.makeText(this, error.getMessage(this) + getString(R.string.setup_check_failed), Style.ALERT, mCroutonHolder).show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mUntrustedCertChain != null) {
			showSSLCertDialog();
		}
	}

	// TODO: better UI
	private void showSSLCertDialog() {
		if (mUntrustedCertChain == null || mUntrustedCertChain.length == 0) {
			return;
		}

		new AlertDialog.Builder(this).setTitle(R.string.auth_accept_cert) //
				.setMessage(mUntrustedCertChain[0].toString()) //
				.setPositiveButton(android.R.string.yes, this) //
						// .setNeutralButton(R.string.mtm_decision_once, this) //
				.setNegativeButton(android.R.string.no, this) //
				.setOnCancelListener(this) //
				.create().show();
	}

	@Override
	public void onClick(final DialogInterface dialog, final int btnId) {
		dialog.dismiss();
		switch (btnId) {
		case DialogInterface.BUTTON_POSITIVE:
			// Save cert
			mKeyStoreDisk.storeCert(mUntrustedCertChain);
			checkServer(null);
			break;

		case DialogInterface.BUTTON_NEUTRAL:
			break;
		default:
		}
	}

	@Override
	public void onCancel(final DialogInterface dialog) {
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (mIsAuthSettingsFragmentShown == false) {
			final MenuInflater i = getSupportMenuInflater();
			i.inflate(R.menu.menu_server, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_server_settings:
			showAdvancedSettingsDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showAdvancedSettingsDialog() {
		mIsAuthSettingsFragmentShown = true;
		supportInvalidateOptionsMenu();
		final FragmentManager fm = getSupportFragmentManager();
		final AdvancedServerSettingsFragment f = AdvancedServerSettingsFragment.newInstance(mAuthUsername, mAuthPassword);

		if (Util.getSmallestScreenWidthDp(this) >= 600) {
			f.show(fm, "serversettings");
		} else {
			final FragmentTransaction ft = fm.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.add(android.R.id.content, f);
			ft.addToBackStack(null).commit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Crouton.cancelAllCroutons();
	}
}
