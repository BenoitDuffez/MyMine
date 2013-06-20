package net.bicou.redmine.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.Project;

import java.util.ArrayList;

/**
 * Created by bicou on 17/06/13.
 */
public class ProjectsSpinnerAdapter extends ArrayAdapter<Project> {
	ArrayList<Project> data;
	LayoutInflater inflater;

	public ProjectsSpinnerAdapter(final Context ctx, final int textViewResourceId, final ArrayList<Project> data) {
		super(ctx, textViewResourceId, data);
		this.data = data;
		inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.main_nav_item_in_actionbar, null);
		}

		final TextView text = (TextView) convertView.findViewById(R.id.main_nav_ab_item_text);

		if (text != null && data != null && position < data.size()) {
			text.setText(data.get(position).name);
		}

		return convertView;
	}

	@Override
	public View getDropDownView(final int position, View convertView, final ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.main_nav_item, null);
		}

		final TextView text = (TextView) convertView.findViewById(R.id.main_nav_item_text);
		final View image = convertView.findViewById(R.id.main_nav_item_icon);

		if (text != null && image != null) {
			text.setText(data.get(position).name);
			image.setVisibility(View.INVISIBLE);
		}

		return convertView;
	}

	@Override
	public int getCount() {
		return data == null ? 0 : data.size();
	}

	@Override
	public Project getItem(final int position) {
		return null;
	}

	@Override
	public long getItemId(final int position) {
		return 0;
	}
}
