package net.bicou.redmine.auth;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import net.bicou.redmine.R;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.ProjectsList;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.net.JsonDownloadError;
import net.bicou.redmine.net.JsonDownloader;
import net.bicou.redmine.net.JsonNetworkError;
import net.bicou.redmine.sync.NetworkUtilities;
import net.bicou.redmine.util.L;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * Represents an asynchronous task used to authenticate a user against the SampleSync Service
 */
public class UserLoginTask {
	public static class UserLoginResult {
		public JsonNetworkError error;
		public Server server;
		public boolean authResult;

		public UserLoginResult() {
			authResult = false;
		}

		public boolean success() {
			return authResult && error == null;
		}
	}

	public static class UserLoginParameters {
		public String serverUrl;
		public String apiKey;
		public String authUsername;
		public String authPassword;

		public UserLoginParameters(final String serverUrl, final String apiKey, final String authUsername, final String authPassword) {
			this.serverUrl = serverUrl;
			this.apiKey = TextUtils.isEmpty(apiKey) ? "" : apiKey.trim();
			this.authUsername = authUsername;
			this.authPassword = authPassword;
		}
	}

	public static UserLoginResult tryUserLogin(Context context, UserLoginParameters params) {
		L.d("params: " + params);
		if (params == null || TextUtils.isEmpty(params.serverUrl)) {
			return null;
		}
		if (TextUtils.isEmpty(params.apiKey)) {
			params.apiKey = ""; // ensure non-null
		}

		UserLoginResult result = new UserLoginResult();

		// Validate server URL
		if (!params.serverUrl.contains("://")) {
			params.serverUrl = "http://" + params.serverUrl;
		}

		// Create server object
		result.server = new Server(params.serverUrl, params.apiKey);
		result.server.authUsername = params.authUsername;
		result.server.authPassword = params.authPassword;

		// Try to log in: download 1st project
		final String url = "projects.json";
		final NameValuePair[] args = new BasicNameValuePair[] { new BasicNameValuePair("limit", "1") };
		result.authResult = false;

		try {
			JsonDownloader<ProjectsList> downloader = new JsonDownloader<ProjectsList>(ProjectsList.class).setDownloadAllIfList(false);
			result.authResult = downloader.fetchObject(context, result.server, url, args) != null;
			result.error = downloader.getError();
		} catch (final Exception e) {
			L.e("Failed to authenticate", e);
			L.i(e.toString());
			result.error = new JsonDownloadError(JsonDownloadError.ErrorType.TYPE_UNKNOWN, e);
			result.error.setMessage(R.string.err_unknown, e.getMessage());
		}

		// Try to get local user info
		if (result.success()) {
			result.server.user = NetworkUtilities.whoAmI(context, result.server);
		}

		return result;
	}

	public static void saveServer(Context context, Server server) {
		L.d("");

		// Save server
		final ServersDbAdapter sdb = new ServersDbAdapter(context);
		sdb.open();
		sdb.insert(server);

		// Save user, if any
		if (server.user != null) {
			final UsersDbAdapter udb = new UsersDbAdapter(sdb);
			udb.insert(server, server.user);
		}
		sdb.close();

		// Show up toast
		final int toastId = server.user != null ? R.string.setup_success : R.string.setup_anonymous;
		final String toast = context.getString(toastId, server.user == null ? "" : server.user.firstname + " " + server.user.lastname);
		Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
	}
}
