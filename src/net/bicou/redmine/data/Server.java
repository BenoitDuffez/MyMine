package net.bicou.redmine.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.Gson;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.ServersDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.util.L;

public class Server implements Parcelable {
	public long rowId;

	public String serverUrl;
	public String apiKey;

	public String authUsername, authPassword;

	public User user;

	//

	public Server(final Parcel source) {
		serverUrl = source.readString();
		apiKey = source.readString();
		authUsername = source.readString();
		authPassword = source.readString();
		user = new Gson().fromJson(source.readString(), User.class);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeString(apiKey);
		dest.writeString(serverUrl);
		dest.writeString(authUsername);
		dest.writeString(authPassword);
		dest.writeString(new Gson().toJson(user));
	}

	public static Parcelable.Creator<Server> CREATOR = new Creator<Server>() {
		@Override
		public Server[] newArray(final int size) {
			return new Server[size];
		}

		@Override
		public Server createFromParcel(final Parcel source) {
			return new Server(source);
		}
	};

	//

	/**
	 * Used to test if a server is valid and readable. Will read and return all projects in this server.
	 *
	 * @author bicou
	 */
	public interface RedmineServerTestCallback {
		public void testResult(Boolean validServer);
	}

	public Server(final String url, final String key) {
		serverUrl = url;
		apiKey = key;
	}

	public Server(final Cursor c) {
		serverUrl = c.getString(c.getColumnIndex(ServersDbAdapter.KEY_SERVER_URL));
		apiKey = c.getString(c.getColumnIndex(ServersDbAdapter.KEY_API_KEY));
		authUsername = c.getString(c.getColumnIndex(ServersDbAdapter.KEY_AUTH_USERNAME));
		authPassword = c.getString(c.getColumnIndex(ServersDbAdapter.KEY_AUTH_PASSWORD));

		if (c.getColumnIndex(DbAdapter.KEY_ROWID) >= 0) {
			rowId = c.getLong(c.getColumnIndex(DbAdapter.KEY_ROWID));
		}
		final int userId = c.getColumnIndex(ServersDbAdapter.KEY_USER_ID) >= 0 ? c.getInt(c.getColumnIndex(ServersDbAdapter.KEY_USER_ID)) : 0;
		if (userId > 0) {
			user = new User(c, UsersDbAdapter.TABLE_USERS + "_");
		}
	}

	public boolean equals(final Server other) {
		L.d("this=" + this + " other=" + other);
		if (serverUrl == null && apiKey == null) {
			if (other == null) {
				return true;
			}
			return other.serverUrl == null && other.apiKey == null;
		}
		if (serverUrl == null) {
			return apiKey.equals(other.apiKey);
		}
		if (apiKey == null) {
			return serverUrl.equals(other.serverUrl);
		}
		// TODO: check username/password?
		return apiKey.equals(other.apiKey) && serverUrl.equals(other.serverUrl);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " { url: " + serverUrl + " }";
	}
}
