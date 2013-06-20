package net.bicou.redmine.app.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

import net.bicou.redmine.R;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class CertificateFragment extends SherlockFragment {
	public static final String KEY_CERT_ALIAS = "net.bicou.redmine.ssl.Certificate";
	X509Certificate mCertificate;
	TextView mStartDate, mEndDate, mIssuer, mSubject, mKey, mAlgo;

	public static CertificateFragment newInstance(final Bundle args) {
		final CertificateFragment f = new CertificateFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_certificate, container, false);

		mStartDate = (TextView) v.findViewById(R.id.cert_start_date);
		mEndDate = (TextView) v.findViewById(R.id.cert_end_date);
		mIssuer = (TextView) v.findViewById(R.id.cert_issuer);
		mSubject = (TextView) v.findViewById(R.id.cert_subject);
		mKey = (TextView) v.findViewById(R.id.cert_key);
		mAlgo = (TextView) v.findViewById(R.id.cert_algo);

		final String alias = getArguments().getString(KEY_CERT_ALIAS);
		final KeyStore ks = new KeyStoreDiskStorage(getActivity()).loadAppKeyStore();
		try {
			mCertificate = (X509Certificate) ks.getCertificate(alias);
		} catch (final KeyStoreException e) {
			// TODO Auto-generated catch block
		}

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mCertificate == null) {
			mStartDate.setText(R.string.not_applicable);
			mEndDate.setText(R.string.not_applicable);
			mIssuer.setText(R.string.not_applicable);
			mSubject.setText(R.string.not_applicable);
			mKey.setText(R.string.not_applicable);
			mAlgo.setText(R.string.not_applicable);
		} else {
			final java.text.DateFormat df = android.text.format.DateFormat.getLongDateFormat(getActivity());
			mStartDate.setText(df.format(mCertificate.getNotBefore()));
			mEndDate.setText(df.format(mCertificate.getNotAfter()));
			mIssuer.setText(mCertificate.getIssuerX500Principal().toString());
			mSubject.setText(mCertificate.getSubjectX500Principal().getName());
			mKey.setText(mCertificate.getPublicKey().toString());
			mAlgo.setText(mCertificate.getSigAlgName());
		}
	}
}
