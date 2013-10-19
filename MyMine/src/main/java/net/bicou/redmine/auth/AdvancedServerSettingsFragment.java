package net.bicou.redmine.auth;

import net.bicou.redmine.R;
import net.bicou.redmine.app.ssl.KeyStoreManagerActivity;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import net.bicou.redmine.app.ga.TrackedDialogFragment;

public class AdvancedServerSettingsFragment extends TrackedDialogFragment {
	ServerSettingsListener mListener;

	public interface ServerSettingsListener {
		void onCredentialsEntered(String username, String password);
	}

	public static final String KEY_USERNAME = "net.bicou.redmine.auth.AdvancedAuthUsername";
	public static final String KEY_PASSWORD = "net.bicou.redmine.auth.AdvancedAuthPassword";
	private String mCurrentUsername, mCurrentPassword;
	EditText mUsername, mPassword;

	public static AdvancedServerSettingsFragment newInstance(final String username, final String password) {
		final AdvancedServerSettingsFragment f = new AdvancedServerSettingsFragment();
		final Bundle args = new Bundle();
		args.putString(KEY_USERNAME, username);
		args.putString(KEY_PASSWORD, password);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!TextUtils.isEmpty(mCurrentUsername)) {
			mUsername.setText(mCurrentUsername);
		}
		if (!TextUtils.isEmpty(mCurrentPassword)) {
			mPassword.setText(mCurrentPassword);
		}
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ServerSettingsListener) {
			mListener = (ServerSettingsListener) activity;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		((AuthenticatorActivity) getActivity()).mIsAuthSettingsFragmentShown = false;
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_server_advanced_settings, container, false);

		mUsername = (EditText) v.findViewById(R.id.server_settings_username);
		mPassword = (EditText) v.findViewById(R.id.server_settings_password);

		v.findViewById(R.id.server_settings_ok_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View clicked) {
				if (mListener != null) {
					mListener.onCredentialsEntered(mUsername.getText().toString(), mPassword.getText().toString());

					final Dialog d = AdvancedServerSettingsFragment.this.getDialog();
					if (d != null) {
						d.dismiss();
					} else {
						getFragmentManager().beginTransaction().remove(AdvancedServerSettingsFragment.this).commit();
					}
				}
			}
		});

		v.findViewById(R.id.server_auth_settings_view_keystore).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View clicked) {
				startActivity(new Intent(getActivity(), KeyStoreManagerActivity.class));
			}
		});

		return v;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		return dialog;
	}
}
