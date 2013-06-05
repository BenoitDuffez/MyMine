package net.bicou.redmine.auth;

import net.bicou.redmine.Constants;
import net.bicou.redmine.util.L;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * This class is an implementation of AbstractAccountAuthenticator for authenticating accounts in the com.example.android.samplesync domain. The interesting thing that this class
 * demonstrates is the use of authTokens as part of the authentication process. In the account setup UI, the user enters their username and password. But for our subsequent calls
 * off to the service for syncing, we want to use an authtoken instead - so we're not continually sending the password over the wire. getAuthToken() will be called when SyncAdapter
 * calls AccountManager.blockingGetAuthToken(). When we get called, we need to return the appropriate authToken for the specified account. If we already have an authToken stored in
 * the account, we return that authToken. If we don't, but we do have a username and password, then we'll attempt to talk to the sample service to fetch an authToken. If that fails
 * (or we didn't have a username/password), then we need to prompt the user - so we create an AuthenticatorActivity intent and return that. That will display the dialog that
 * prompts the user for their login information.
 */
class Authenticator extends AbstractAccountAuthenticator {
	// Authentication Service context
	private final Context mContext;

	public Authenticator(final Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public Bundle addAccount(final AccountAuthenticatorResponse response, final String accountType, final String authTokenType, final String[] requiredFeatures,
			final Bundle options) {
		L.v("addAccount()");
		final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle confirmCredentials(final AccountAuthenticatorResponse response, final Account account, final Bundle options) {
		L.v("confirmCredentials()");
		return null;
	}

	@Override
	public Bundle editProperties(final AccountAuthenticatorResponse response, final String accountType) {
		L.v("editProperties()");
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle getAuthToken(final AccountAuthenticatorResponse response, final Account account, final String authTokenType, final Bundle loginOptions)
			throws NetworkErrorException {
		L.v("getAuthToken()");

		// If the caller requested an authToken type we don't support, then
		// return an error
		if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}

		// Extract the username and password from the Account Manager, and ask
		// the server for an appropriate AuthToken.
		final AccountManager am = AccountManager.get(mContext);
		final String apiKey = am.getPassword(account);
		if (apiKey != null) {
			final String authToken = apiKey;// TODO: check? NetworkUtilities.authenticate(account.name, apiKey, mContext);
			if (authToken != null) {
				final Bundle result = new Bundle();
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
				result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
				return result;
			}
		}

		// If we get here, then we couldn't access the user's password - so we
		// need to re-prompt them for their credentials. We do that by creating
		// an intent to display our AuthenticatorActivity panel.
		final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
		intent.putExtra(AuthenticatorActivity.PARAM_SERVER_URL, account.name);
		intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public String getAuthTokenLabel(final String authTokenType) {
		// null means we don't support multiple authToken types
		L.v("getAuthTokenLabel()");
		return null;
	}

	@Override
	public Bundle hasFeatures(final AccountAuthenticatorResponse response, final Account account, final String[] features) {
		// This call is used to query whether the Authenticator supports
		// specific features. We don't expect to get called, so we always
		// return false (no) for any queries.
		L.v("hasFeatures()");
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(final AccountAuthenticatorResponse response, final Account account, final String authTokenType, final Bundle loginOptions) {
		L.v("updateCredentials()");
		return null;
	}
}
