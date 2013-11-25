package net.bicou.redmine.app.issues.order;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;
import net.bicou.redmine.util.PreferencesManager;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by bicou on 22/06/13.
 */
public class IssuesOrder {
	public static final String KEY_HAS_COLUMNS_ORDER = "net.bicou.redmine.app.issues.order.HasOrder";
	public static final String KEY_COLUMNS_ORDER = "net.bicou.redmine.app.issues.order.ColumnsOrder";

	protected ArrayList<OrderColumn> columns;

	private final static Type ORDER_TYPE = new TypeToken<ArrayList<OrderColumn>>() {
	}.getType();

	public static final IssuesOrder DEFAULT_ORDER = new IssuesOrder() {
		{
			columns = new ArrayList<OrderColumn>() {
				{
					add(new OrderColumn(IssuesDbAdapter.KEY_ID, false));
				}
			};
		}
	};

	private IssuesOrder() {
	}

	public static IssuesOrder fromJson(String json) {
		IssuesOrder order = new Gson().fromJson(json, IssuesOrder.class);
		if (order == null) {
			order = DEFAULT_ORDER;
		}
		return order;
	}

	public static IssuesOrder fromList(ArrayList<OrderColumn> cols) {
		IssuesOrder order = new IssuesOrder();
		order.columns = cols;
		return order;
	}

	public static IssuesOrder fromBundle(Bundle data) {
		if (data == null) {
			return DEFAULT_ORDER;
		}
		if (data.getBoolean(KEY_HAS_COLUMNS_ORDER)) {
			IssuesOrder order = new IssuesOrder();
			order.columns = data.getParcelableArrayList(KEY_COLUMNS_ORDER);
			return order;
		}
		return DEFAULT_ORDER;
	}

	public static IssuesOrder fromPreferences(Context ctx) {
		IssuesOrder order = null;

		final String json = PreferencesManager.getString(ctx, IssuesOrder.KEY_COLUMNS_ORDER, null);
		if (!TextUtils.isEmpty(json)) {
			try {
				order = IssuesOrder.fromJson(json);
			} catch (final Exception e) {
				PreferencesManager.setString(ctx, IssuesOrder.KEY_COLUMNS_ORDER, null);
			}
		}

		if (order == null) {
			return IssuesOrder.DEFAULT_ORDER;
		}

		return order;
	}

	public void saveTo(Bundle args) {
		if (args != null) {
			args.putBoolean(KEY_HAS_COLUMNS_ORDER, !isEmpty());
			args.putParcelableArrayList(KEY_COLUMNS_ORDER, columns);
		}
	}

	public void saveToPreferences(Context ctx) {
		final String json = new Gson().toJson(columns, ORDER_TYPE);
		PreferencesManager.setString(ctx, KEY_COLUMNS_ORDER, json);
	}

	public boolean isEmpty() {
		return columns == null || columns.size() <= 0;
	}

	public ArrayList<OrderColumn> getColumns() {
		return columns;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " { columns: " + columns + " }";
	}
}
