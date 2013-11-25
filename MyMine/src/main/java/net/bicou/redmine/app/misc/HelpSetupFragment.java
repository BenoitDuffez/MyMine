package net.bicou.redmine.app.misc;

import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.util.L;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class HelpSetupFragment extends TrackedFragment {
	Button button;
	EditText url, apiKey;
	Server mServer;
	boolean mEditMode;

	public static HelpSetupFragment newInstance(final Bundle args) {
		final HelpSetupFragment frag = new HelpSetupFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		L.d("");
		final View fragmentLayout = inflater.inflate(R.layout.frag_help, container, false);
		fragmentLayout.findViewById(R.id.setup_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final Intent intent = getNewAccountActivityIntent();
				try {
					getActivity().startActivity(intent);
				} catch (final Exception e) {
					L.e("Couldn't get to settings because of a " + e.toString());
					Toast.makeText(getActivity(), R.string.setup_sync_help, Toast.LENGTH_LONG).show();
				}
			}
		});

		return fragmentLayout;
	}

	public static Intent getNewAccountActivityIntent() {
		final Intent intent = new Intent();
		final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.accounts.AddAccountSettings");
		intent.setClassName("com.android.settings", ".accounts.AddAccountsSettings");
		intent.setComponent(cn);
		return intent;
	}
}
