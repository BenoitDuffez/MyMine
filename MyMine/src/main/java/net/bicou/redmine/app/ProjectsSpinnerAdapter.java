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
	LayoutInflater inflater;

	public ProjectsSpinnerAdapter(final Context ctx, final ArrayList<Project> data) {
		super(ctx, R.layout.main_nav_item, 0, data);
		inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.main_nav_item_in_actionbar, null);
		}

		final TextView text = (TextView) convertView.findViewById(R.id.main_nav_ab_item_text);
		Project project = getItem(position);

		if (text != null && project != null) {
			text.setText(project.name);
		}

		return convertView;
	}

	@Override
	public View getDropDownView(final int position, View convertView, final ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.main_nav_item, null);
		}

		final TextView text = (TextView) convertView.findViewById(R.id.main_nav_item_text);
		Project project = getItem(position);

		if (text != null && project != null) {
			text.setText(project.name);
		}

		return convertView;
	}

	@Override
	public long getItemId(final int position) {
		Project item = getItem(position);
		return item == null ? 0 : item.id;
	}
}
