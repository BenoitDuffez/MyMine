package net.bicou.redmine.data.json;

import android.database.Cursor;
import android.database.SQLException;
import android.text.TextUtils;

import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class User {
	public long id;
	public String firstname;
	public String lastname;
	public String mail;
	public Calendar created_on;
	public Calendar last_login_on;
	public String login;

	// Not from json:
	public String gravatarUrl;

	public boolean createGravatarUrl() {
		gravatarUrl = "";

		if (mail == null) {
			mail = "";
		}

		MessageDigest md;
		byte[] digest = null;
		try {
			md = MessageDigest.getInstance("MD5");
			digest = md.digest(mail.toLowerCase(Locale.ENGLISH).trim().getBytes("UTF-8"));
			md.reset();
		} catch (final NoSuchAlgorithmException e) {
			return false;
		} catch (final UnsupportedEncodingException e) {
			return false;
		}

		final BigInteger bigInt = new BigInteger(1, digest);
		String md5 = bigInt.toString(16);

		// Now we need to zero pad it to get the full 32 chars.
		while (md5.length() < 32) {
			md5 = "0" + md5;
		}

		if (TextUtils.isEmpty(md5)) {
			return false;
		}

		gravatarUrl = String.format("http://www.gravatar.com/avatar/%s?d=identicon", md5);

		return !TextUtils.isEmpty(gravatarUrl);
	}

	public String getName() {
		if (firstname == null) {
			firstname = "";
		}
		if (lastname == null) {
			lastname = "";
		}
		if (TextUtils.isEmpty(firstname) && TextUtils.isEmpty(lastname)) {
			return "";
		}
		return String.format(Locale.ENGLISH, "%s %s", firstname, lastname);
	}

	public User() {
	}

	public User(final Cursor c) {
		this(c, "");
	}

	public User(Cursor c, String columnPrefix) {
		for (final String col : UsersDbAdapter.USER_FIELDS) {
			final int columnIndex = c.getColumnIndex(columnPrefix + col);
			if (columnIndex < 0) {
				continue;
			}

			try {
				if (col.equals(UsersDbAdapter.KEY_ID)) {
					id = c.getInt(columnIndex);
				} else if (col.equals(UsersDbAdapter.KEY_FIRSTNAME)) {
					firstname = c.getString(columnIndex);
				} else if (col.equals(UsersDbAdapter.KEY_LASTNAME)) {
					lastname = c.getString(columnIndex);
				} else if (col.equals(UsersDbAdapter.KEY_MAIL)) {
					mail = c.getString(columnIndex);
				} else if (col.equals(UsersDbAdapter.KEY_CREATED_ON)) {
					created_on = new GregorianCalendar();
					created_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(UsersDbAdapter.KEY_LAST_LOGIN_ON)) {
					last_login_on = new GregorianCalendar();
					last_login_on.setTimeInMillis(c.getLong(columnIndex));
				} else if (col.equals(UsersDbAdapter.KEY_SERVER_ID)) {
					// TODO
				} else {
					L.e("Unhandled column! " + col, null);
				}
			} catch (final SQLException e) {
				L.e("unable to retrieve column " + col, e);
			}
		}
	}

	@Override
	public String toString() {
		return super.toString() + " " + getClass().getSimpleName() + " { #" + id + ", login: " + login + ", name: " + getName() + ", mail:" + mail + ", " +
				"avatar: " + gravatarUrl + ", " + "created/last login: " + (Util.isEpoch(created_on) ? "epoch" : created_on.getTime()) + "/" + (Util.isEpoch(last_login_on) ? "epoch" : last_login_on.getTime()) + " }";
	}
}
