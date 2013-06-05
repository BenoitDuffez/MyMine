package net.bicou.redmine.auth;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.auth.AdvancedServerSettingsFragment.ServerSettingsListener;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.ProjectsList;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.net.JsonDownloadError;
import net.bicou.redmine.net.JsonDownloadError.ErrorType;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.sync.NetworkUtilities;
import net.bicou.redmine.sync.SyncUtils;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
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

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorSherlockActivity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, ServerSettingsListener {
	/** The Intent flag to confirm credentials. */
	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

	/** The Intent extra to store apiKey. */
	public static final String PARAM_APIKEY = "apiKey";

	/** The Intent extra to store serverUrl. */
	public static final String PARAM_SERVER_URL = "serverUrl";

	/** The Intent extra to store serverUrl. */
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	private AccountManager mAccountManager;

	/** Keep track of the login task so can cancel it if requested */
	private UserLoginTask mAuthTask = null;

	/** Was the original caller asking for an entirely new account? */
	protected boolean mRequestNewAccount = false;

	private String mApiKey;
	private EditText mApiKeyEdit;

	private String mServerUrl;
	private EditText mServerUrlEdit;

	private JsonDownloadError mError;
	KeyStoreDiskStorage mKeyStoreDisk;

	private static final String KEY_SERVER_URL = "net.bicou.redmine.auth.ServerURL";
	private static final String KEY_SERVER_API_KEY = "net.bicou.redmine.auth.ServerAPIKey";

	public boolean mIsAuthSettingsFragmentShown = false;
	private String mAuthUsername, mAuthPassword;
	private ViewGroup mCroutonHolder;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setProgressBarIndeterminateVisibility(false);

		mAccountManager = AccountManager.get(this);
		mKeyStoreDisk = new KeyStoreDiskStorage(AuthenticatorActivity.this);

		// Load data from intent
		final Intent intent = getIntent();
		mServerUrl = intent.getStringExtra(PARAM_SERVER_URL);
		mRequestNewAccount = mServerUrl == null;
		L.i("    request new: " + mRequestNewAccount);

		// Handle layout and views
		setContentView(R.layout.activity_server);
		mCroutonHolder = (ViewGroup) findViewById(R.id.setup_crouton_holder);
		mServerUrlEdit = (EditText) findViewById(R.id.setup_server_url);
		mApiKeyEdit = (EditText) findViewById(R.id.setup_api_key);
		if (!TextUtils.isEmpty(mServerUrl)) {
			mServerUrlEdit.setText(mServerUrl);
		}

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
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SERVER_URL, mServerUrlEdit.getText().toString());
		outState.putString(KEY_SERVER_API_KEY, mApiKeyEdit.getText().toString());
	}

	/**
	 * Handles onClick event on the Submit button. Sends serverUrl/apiKey to the server for authentication. The button is configured to call
	 * handleLogin() in the layout XML.
	 * 
	 * @param view
	 *            The Submit button for which this method is invoked
	 */
	public void checkServer(final View view) {
		if (mRequestNewAccount) {
			mServerUrl = mServerUrlEdit.getText().toString();
		}
		mApiKey = mApiKeyEdit.getText().toString();

		// Show a progress dialog, and kick off a background task to perform
		// the user login attempt.
		setProgressBarIndeterminateVisibility(true);
		mAuthTask = new UserLoginTask();
		mAuthTask.execute();
	}

	/**
	 * Called when response is received from the server for authentication request. See onAuthenticationResult(). Sets the AccountAuthenticatorResult
	 * which is sent back to the caller. We store the authToken that's returned from the server as the 'apiKey' for this account - so we're never
	 * storing the user's actual apiKey locally.
	 * 
	 * @param result
	 *            the confirmCredentials result.
	 */
	private void onLoginSuccessful() {
		L.d("");
		final Account account = new Account(mServerUrl, Constants.ACCOUNT_TYPE);
		if (mRequestNewAccount) {
			mAccountManager.addAccountExplicitly(account, mApiKey, null);
			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
		} else {
			mAccountManager.setPassword(account, mApiKey);
		}

		saveServer();
		SyncUtils.enableSync(account, this);

		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mServerUrl);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	Server mServer;

	/**
	 * Ran in the background, in order do DB management
	 * 
	 * @param projectsList
	 */
	private void saveServer() {
		L.d("");

		// Save server
		final ServersDbAdapter sdb = new ServersDbAdapter(this);
		sdb.open();
		sdb.insert(mServer);
		sdb.close();

		// Save user, if any
		if (mServer.user != null) {
			final UsersDbAdapter udb = new UsersDbAdapter(this);
			udb.open();
			udb.insert(mServer, mServer.user);
			udb.close();
		}

		// Show up toast
		final int toastId = mServer.user != null ? R.string.setup_success : R.string.setup_anonymous;
		final String toast = getString(toastId, mServer.user == null ? "" : mServer.user.firstname + " " + mServer.user.lastname);
		Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
	}

	private void onLoginFailed() {
		L.e("onAuthenticationResult: failed to authenticate");
		if (mRequestNewAccount) {
			final String errorMessage;

			switch (mError.errorType) {
			case TYPE_NETWORK:
				final String details;
				if (mError.exception == null) {
					details = mError.getMessage(this);
				} else {
					details = mError.exception.getClass().getSimpleName() + mError.exception.getMessage() == null ? //
					mError.exception.getCause().toString()
							: mError.exception.getMessage();
				}
				errorMessage = getString(mError.errorType == ErrorType.TYPE_JSON ? R.string.auth_error_json : R.string.auth_error_network, details);
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

			Crouton.makeText(this, errorMessage + "\n" + getString(R.string.setup_button_tryagain), Style.ALERT, mCroutonHolder).show();
		} else {
			Crouton.makeText(this, mError.getMessage(this) + getString(R.string.setup_check_failed), Style.ALERT, mCroutonHolder).show();
		}

		if (mError != null && mError.chain != null) {
			showSSLCertDialog();
		}
	}

	/**
	 * Called when the authentication process completes (see attemptLogin()).
	 * 
	 * @param authToken
	 *            the authentication token returned by the server, or NULL if authentication failed.
	 */
	public void onAuthenticationResult(final boolean success) {
		L.d("");
		// Our task is complete, so clear it out
		mAuthTask = null;
		setProgressBarIndeterminateVisibility(false);

		if (success) {
			onLoginSuccessful();
		} else {
			onLoginFailed();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mError != null && mError.chain != null) {
			showSSLCertDialog();
		}
	}

	// TODO: better UI
	private void showSSLCertDialog() {
		new AlertDialog.Builder(this).setTitle(R.string.auth_accept_cert) //
				.setMessage(mError.chain[0].toString()) //
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
			mKeyStoreDisk.storeCert(mError.chain);
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

	public void onAuthenticationCancel() {
		L.d("");
		mAuthTask = null;
		setProgressBarIndeterminateVisibility(false);
	}

	/**
	 * Represents an asynchronous task used to authenticate a user against the SampleSync Service
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		JsonDownloader<?> mTask;

		@Override
		protected Boolean doInBackground(final Void... params) {
			// Validate server URL
			if (mServerUrl.indexOf("://") < 0) {
				mServerUrl = "http://" + mServerUrl;
			}

			mServer = new Server(mServerUrl, mApiKey);
			mServer.authUsername = mAuthUsername;
			mServer.authPassword = mAuthPassword;

			final String url = "projects.json";
			final NameValuePair[] args = new BasicNameValuePair[] {
				new BasicNameValuePair("limit", "1")
			};
			boolean authResult = false;

			try {
				mTask = new JsonDownloader<ProjectsList>(ProjectsList.class).setDownloadAllIfList(false);
				authResult = mTask.fetchObject(AuthenticatorActivity.this, mServer, url, args) != null;
				mError = mTask.getError();
			} catch (final Exception e) {
				L.e("Failed to authenticate", e);
				L.i(e.toString());
				mError = new JsonDownloadError(ErrorType.TYPE_UNKNOWN, e);
				mError.setMessage(R.string.err_unknown, e.getMessage());
			}

			if (authResult && mError == null) {
				mServer.user = NetworkUtilities.whoAmI(AuthenticatorActivity.this, mServer);
			}

			return authResult && mError == null;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			onAuthenticationResult(success);
		}

		@Override
		protected void onCancelled() {
			onAuthenticationCancel();
		}
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

	@Override
	public void onCredentialsEntered(final String username, final String password) {
		mAuthUsername = username;
		mAuthPassword = password;
	}
}
