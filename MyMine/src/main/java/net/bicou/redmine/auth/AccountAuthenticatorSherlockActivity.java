package net.bicou.redmine.auth;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class AccountAuthenticatorSherlockActivity extends ActionBarActivity {
	private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
	private Bundle mResultBundle = null;

	public final void setAccountAuthenticatorResult(Bundle result) {
		mResultBundle = result;
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

		if (mAccountAuthenticatorResponse != null) {
			mAccountAuthenticatorResponse.onRequestContinued();
		}
	}

	@Override
	public void finish() {
		if (mAccountAuthenticatorResponse != null) {
			if (mResultBundle != null) {
				mAccountAuthenticatorResponse.onResult(mResultBundle);
			} else {
				mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
			}
			mAccountAuthenticatorResponse = null;
		}
		super.finish();
	}
}
