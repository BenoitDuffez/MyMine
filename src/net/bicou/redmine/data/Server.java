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
		rowId = source.readLong();
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
		dest.writeLong(rowId);
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

	public Server(final String url, final String key) {
		serverUrl = url;
		apiKey = key;
	}

	public Server(final Cursor c) {
		this(c, "");
	}

	public Server(final Cursor c, String columnPrefix) {
		int colIndex;
		for (String col : ServersDbAdapter.SERVER_FIELDS) {
			colIndex = c.getColumnIndex(columnPrefix + col);
			if (colIndex < 0) {
				continue;
			}
			if (ServersDbAdapter.KEY_SERVER_URL.equals(col)) {
				serverUrl = c.getString(colIndex);
			} else if (ServersDbAdapter.KEY_API_KEY.equals(col)) {
				apiKey = c.getString(colIndex);
			} else if (ServersDbAdapter.KEY_AUTH_USERNAME.equals(col)) {
				authUsername = c.getString(colIndex);
			} else if (ServersDbAdapter.KEY_AUTH_PASSWORD.equals(col)) {
				authPassword = c.getString(colIndex);
			} else if (DbAdapter.KEY_ROWID.equals(col)) {
				rowId = c.getLong(colIndex);
			} else if (ServersDbAdapter.KEY_USER_ID.equals(col)) {
				final int userId = c.getColumnIndex(ServersDbAdapter.KEY_USER_ID) >= 0 ? c.getInt(c.getColumnIndex(ServersDbAdapter.KEY_USER_ID)) : 0;
				if (userId > 0) {
					user = new User(c, UsersDbAdapter.TABLE_USERS + "_");
				}
			}
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
		return this.getClass().getSimpleName() + " { #" + rowId + ", url: " + serverUrl + " }";
	}
}
