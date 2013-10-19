/*
 * KeyStoreDiskStorage - a TrustManager which asks the user about invalid certificates and memorizes their decision. Copyright (c) 2010 Georg Lukas <georg@op-co.de>
 * KeyStoreDiskStorage.java contains the actual trust manager and interface code to create a MemorizingActivity and obtain the results. Permission is hereby
 * granted, free of
 * charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of
  * the Software. THE
 * SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.bicou.redmine.net.ssl;

import android.content.Context;
import net.bicou.redmine.util.L;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * A X509 trust manager implementation which asks the user about invalid certificates and memorizes their decision.
 * <p/>
 * The certificate validity is checked using the system default X509 TrustManager, creating a query Dialog if the check fails.
 * <p/>
 * <b>WARNING:</b> This only works if a dedicated thread is used for opening sockets!
 */
public class KeyStoreDiskStorage {
	static String KEYSTORE_DIR = "private";
	static String KEYSTORE_FILE = "KeyStore.bks";

	private final File mKeyStoreFilePath;

	private static final String password = "Catch" + "MyMine".toCharArray()[2] + "e";


	/**
	 * Creates an instance of the KeyStoreDiskStorage class.
	 * <p/>
	 * You need to supply the application context. This has to be one of: - Application - Activity - Service
	 * <p/>
	 * The context is used for file management, to display the dialog / notification and for obtaining translated strings.
	 *
	 * @param m Context for the application.
	 */
	public KeyStoreDiskStorage(final Context m) {
		final File dir = m.getDir(KEYSTORE_DIR, Context.MODE_PRIVATE);
		mKeyStoreFilePath = new File(dir + File.separator + KEYSTORE_FILE);
	}

	public KeyStore loadAppKeyStore() {
		KeyStore ks;
		try {
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch (final KeyStoreException e) {
			L.e("getAppKeyStore()", e);
			return null;
		}

		try {
			ks.load(null, null);
			ks.load(new java.io.FileInputStream(mKeyStoreFilePath), password.concat("IfYouCan").toCharArray());
		} catch (final java.io.FileNotFoundException e) {
			L.i("getAppKeyStore(" + mKeyStoreFilePath + ") - file does not exist");
		} catch (final Exception e) {
			L.e("getAppKeyStore(" + mKeyStoreFilePath + ")", e);
		}

		return ks;
	}

	public void storeCert(final X509Certificate[] chain) {
		KeyStore appKeyStore = loadAppKeyStore();

		// Add all certs from chain to appKeyStore
		try {
			for (final X509Certificate c : chain) {
				appKeyStore.setCertificateEntry(c.getSubjectDN().toString(), c);
			}
		} catch (final KeyStoreException e) {
			L.e("storeCert(" + Arrays.toString(chain) + ")", e);
			return;
		}

		// Store KeyStore to file
		java.io.FileOutputStream fos = null;
		try {
			fos = new java.io.FileOutputStream(mKeyStoreFilePath);
			appKeyStore.store(fos, password.concat("IfYouCan").toCharArray());
			fos.close();
		} catch (final Exception e) {
			L.e("storeCert(" + mKeyStoreFilePath + ")", e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
