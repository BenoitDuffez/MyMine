package net.bicou.redmine.app.ssl;

import net.bicou.redmine.util.L;

import java.security.cert.X509Certificate;

/**
* Created by bicou on 15/07/13.
*/
public class CertInfo {
	KeyStoreLoader mLoader;
	X509Certificate mCertificate;
	String mAlias;

	public CertInfo(final KeyStoreLoader loader, final X509Certificate cert, final String alias) {
		L.d("");
		mLoader = loader;
		mCertificate = cert;
		mAlias = alias;
	}

	public String getLabel() {
		if (mCertificate != null) {
			return mCertificate.getIssuerX500Principal().toString();
		}
		return "";
	}

	public String getAlias() {
		return mAlias;
	}
}
