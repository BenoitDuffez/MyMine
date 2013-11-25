package net.bicou.redmine.app.ssl;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.net.ssl.MyMineSSLKeyManager;
import net.bicou.redmine.util.L;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
* Created by bicou on 15/07/13.
*/
public class KeyStoreLoader extends AsyncTaskLoader<List<CertInfo>> {
	Context mContext;
	List<CertInfo> mData;

	public KeyStoreLoader(final Context context) {
		super(context);
		L.d("");
		mContext = context;
	}

	@Override
	public List<CertInfo> loadInBackground() {
		mData = new ArrayList<CertInfo>();

		loadFromAppKeyStore();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			loadFromSystem();
		}

		return mData;
	}

	private void loadFromAppKeyStore() {
		final KeyStoreDiskStorage ds = new KeyStoreDiskStorage(mContext);
		final KeyStore keyStore = ds.loadAppKeyStore();
		try {
			final Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				final String alias = aliases.nextElement();
				final X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
				mData.add(new CertInfo(this, cert, alias));
			}
		} catch (final KeyStoreException e) {
			// TODO
		} catch (final ClassCastException e) {
			// TODO
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void loadFromSystem() {
		try {
			final MyMineSSLKeyManager km = MyMineSSLKeyManager.fromAlias(mContext);
			if (km == null) {
				return;
			}

			final X509Certificate[] certs = km.getCertificateChain(null);
			if (certs == null) {
				return;
			}

			final String alias = km.chooseClientAlias(null, null, null);
			if (alias == null) {
				return;
			}

			for (final X509Certificate cert : certs) {
				mData.add(new CertInfo(this, cert, alias));
			}
		} catch (final CertificateException e) {
			// TODO
		}
	}

	@Override
	protected void onStartLoading() {
		if (mData != null) {
			deliverResult(mData);
		} else {
			forceLoad();
		}
	}
}
