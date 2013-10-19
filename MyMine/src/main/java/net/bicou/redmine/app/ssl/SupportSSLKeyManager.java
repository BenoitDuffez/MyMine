package net.bicou.redmine.app.ssl;

import net.bicou.redmine.net.ssl.MyMineSSLKeyManager;
import net.bicou.redmine.util.L;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

public class SupportSSLKeyManager {
	public static MyMineSSLKeyManager init(final Context ctx) {
		if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
			return doInit(ctx);
		}
		return null;
	}

	private static MyMineSSLKeyManager doInit(final Context ctx) {
		try {
			return MyMineSSLKeyManager.fromAlias(ctx);
		} catch (final Exception e) {
			L.e("Can't init SSL!", e);
		}
		return null;
	}
}
