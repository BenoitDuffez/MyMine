package net.bicou.redmine.app.issues.order;

import android.os.Parcel;
import android.os.Parcelable;

/**
* Created by bicou on 22/06/13.
*/
public class OrderColumn implements Parcelable {
	public String key;
	public boolean isAscending;

	public OrderColumn(final String k, final boolean asc) {
		key = k;
		isAscending = asc;
	}

	public OrderColumn(final Parcel in) {
		key = in.readString();
		isAscending = in.readInt() > 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeString(key);
		dest.writeInt(isAscending ? 1 : 0);
	}

	public static final Creator<OrderColumn> CREATOR = new Creator<OrderColumn>() {
		@Override
		public OrderColumn createFromParcel(final Parcel in) {
			return new OrderColumn(in);
		}

		@Override
		public OrderColumn[] newArray(final int size) {
			return new OrderColumn[size];
		}
	};

	@Override
	public String toString() {
		return "OrderColumn { key: " + key + ", isAscending: " + isAscending + " }";
	}
}
