package net.bicou.redmine.net.ssl;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.text.TextUtils;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.PreferencesManager;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A KeyManager that reads uses credentials stored in the system KeyChain.
 *
 * @author bicou
 *
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MyMineSSLKeyManager implements X509KeyManager {
	private final String mClientAlias;
	private final X509Certificate[] mCertificateChain;
	private final PrivateKey mPrivateKey;

	public static final String KEY_CERTIFICATE_ALIAS = "net.bicou.redmine.net.ssl.CertificateAlias";

	/**
	 * Builds an instance of a KeyChainKeyManager using the given certificate alias. If for any reason retrieval of the credentials from the system
	 * KeyChain fails, a null value will be returned.
	 *
	 * @param context
	 * @return
	 * @throws CertificateException
	 */
	public static MyMineSSLKeyManager fromAlias(final Context context) throws CertificateException {
		String alias = PreferencesManager.getString(context, KEY_CERTIFICATE_ALIAS, null);

		if (TextUtils.isEmpty(alias)) {
			return null;
		}

		X509Certificate[] certificateChain = getCertificateChain(context, alias);
		PrivateKey privateKey = getPrivateKey(context, alias);

		if (certificateChain == null || privateKey == null) {
			throw new CertificateException("Can't access certificate from keystore");
		}

		return new MyMineSSLKeyManager(alias, certificateChain, privateKey);
	}

	public static X509Certificate[] getCertificateChain(Context context, final String alias) throws CertificateException {
		X509Certificate[] certificateChain;
		try {
			certificateChain = KeyChain.getCertificateChain(context, alias);
		} catch (final KeyChainException e) {
			logError(alias, "certificate chain", e);
			throw new CertificateException(e);
		} catch (final InterruptedException e) {
			logError(alias, "certificate chain", e);
			throw new CertificateException(e);
		}
		return certificateChain;
	}

	public static PrivateKey getPrivateKey(Context context, String alias) throws CertificateException {
		PrivateKey privateKey;
		try {
			privateKey = KeyChain.getPrivateKey(context, alias);
		} catch (final KeyChainException e) {
			logError(alias, "private key", e);
			throw new CertificateException(e);
		} catch (final InterruptedException e) {
			logError(alias, "private key", e);
			throw new CertificateException(e);
		}

		return privateKey;
	}

	private static void logError(final String alias, final String type, final Exception ex) {
		L.e("Unable to retrieve " + type + " for [" + alias + "] due to " + ex);
	}

	private MyMineSSLKeyManager(final String clientAlias, final X509Certificate[] certificateChain, final PrivateKey privateKey) {
		mClientAlias = clientAlias;
		mCertificateChain = certificateChain;
		mPrivateKey = privateKey;
	}

	@Override
	public String chooseClientAlias(final String[] keyType, final Principal[] issuers, final Socket socket) {
		return mClientAlias;
	}

	@Override
	public X509Certificate[] getCertificateChain(final String alias) {
		return mCertificateChain;
	}

	@Override
	public PrivateKey getPrivateKey(final String alias) {
		return mPrivateKey;
	}

	@Override
	public String[] getClientAliases(final String keyType, final Principal[] issuers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getServerAliases(final String keyType, final Principal[] issuers) {
		throw new UnsupportedOperationException();
	}

}
