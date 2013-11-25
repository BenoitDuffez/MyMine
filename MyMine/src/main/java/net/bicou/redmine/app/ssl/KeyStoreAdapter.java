package net.bicou.redmine.app.ssl;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import net.bicou.redmine.R;

import java.util.List;

/**
* Created by bicou on 15/07/13.
*/
public class KeyStoreAdapter extends ArrayAdapter<CertInfo> {
	LayoutInflater mInflater;

	public KeyStoreAdapter(final Context context) {
		super(context, R.layout.listview_item);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view;

		if (convertView == null) {
			view = mInflater.inflate(R.layout.listview_item, parent, false);
		} else {
			view = convertView;
		}

		final CertInfo item = getItem(position);
		((TextView) view.findViewById(android.R.id.text1)).setText(item.getLabel());

		return view;
	}

	public String getAlias(final int position) {
		final CertInfo item = getItem(position);
		if (item != null) {
			return item.getAlias();
		}
		return null;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setData(final List<CertInfo> data) {
		clear();
		if (data != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				addAll(data);
			} else {
				for (final CertInfo item : data) {
					add(item);
				}
			}
		}
	}
}
