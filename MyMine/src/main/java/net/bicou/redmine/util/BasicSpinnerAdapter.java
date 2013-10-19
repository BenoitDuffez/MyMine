package net.bicou.redmine.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import net.bicou.redmine.R;

import java.util.List;

/**
 * Created by bicou on 07/08/13.
 */
public abstract class BasicSpinnerAdapter<T> extends ArrayAdapter<T> {
	abstract public String getText(T item);

	public BasicSpinnerAdapter(Context ctx, List<T> items) {
		super(ctx, android.R.layout.simple_list_item_1, android.R.id.text1, items);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup viewGroup) {
		TextView tv;
		View v;
		if (convertView == null) {
			v = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, viewGroup, false);
			tv = (TextView) v.findViewById(android.R.id.text1);
			v.setTag(tv);
		} else {
			v = convertView;
			tv = (TextView) v.getTag();
		}

		T item = getItem(position);
		if (item == null) {
			tv.setText(getContext().getString(R.string.issue_edit_spinner_no_change));
		} else {
			tv.setText(getText(item));
		}

		return v;
	}

	@Override
	public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
		return getView(position, convertView, parent);
	}
}
