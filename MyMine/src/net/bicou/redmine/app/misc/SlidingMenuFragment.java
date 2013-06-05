package net.bicou.redmine.app.misc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AbsMyMineActivity;
import net.bicou.redmine.app.issues.IssuesActivity;
import net.bicou.redmine.app.projects.ProjectsActivity;
import net.bicou.redmine.app.roadmap.RoadmapActivity;
import net.bicou.redmine.app.settings.SettingsActivity;
import net.bicou.redmine.app.wiki.WikiActivity;
import net.bicou.redmine.util.L;

public class SlidingMenuFragment extends SherlockListFragment {
	static class SlidingMenuItem {
		int image, text;

		public SlidingMenuItem(final int image, final int text) {
			this.image = image;
			this.text = text;
		}
	}

	static class SlidingMenuItemViewsHolder {
		ImageView icon;
		TextView text;
	}

	private static final SlidingMenuItem[] mMenu = {
			new SlidingMenuItem(R.drawable.icon_projects, R.string.menu_projects),
			new SlidingMenuItem(R.drawable.icon_issues, R.string.menu_issues),
			new SlidingMenuItem(R.drawable.icon_roadmaps, R.string.menu_roadmap),
			new SlidingMenuItem(R.drawable.icon_wiki, R.string.menu_wiki),
			new SlidingMenuItem(R.drawable.icon_about, R.string.menu_about),
			new SlidingMenuItem(R.drawable.icon_settings, R.string.menu_settings),
	};

	public static SlidingMenuFragment newInstance() {
		final SlidingMenuFragment frag = new SlidingMenuFragment();
		return frag;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		L.d("");
		return inflater.inflate(R.layout.slidingmenu_listview, null);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(new SlidingMenuItemsAdapter(getActivity(), R.layout.slidingmenu_item, R.id.slidingmenu_item_text, mMenu));
	}

	private static class SlidingMenuItemsAdapter extends ArrayAdapter<SlidingMenuItem> {
		public SlidingMenuItemsAdapter(final Context context, final int resource, final int textViewResourceId, final SlidingMenuItem[] objects) {
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			final SlidingMenuItemViewsHolder h;
			final View v;

			if (convertView == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.slidingmenu_item, null);
				h = new SlidingMenuItemViewsHolder();
				h.icon = (ImageView) v.findViewById(R.id.slidingmenu_item_icon);
				h.text = (TextView) v.findViewById(R.id.slidingmenu_item_text);
				v.setTag(h);
			} else {
				v = convertView;
				h = (SlidingMenuItemViewsHolder) v.getTag();
			}

			final SlidingMenuItem item = getItem(position);
			if (item != null) {
				h.icon.setImageResource(item.image);
				h.text.setText(item.text);
			}

			return v;
		}
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Bundle args = new Bundle();
		final Intent intent;
		final AbsMyMineActivity act = (AbsMyMineActivity) getActivity();
		args.putInt(Constants.KEY_PROJECT_POSITION, act.mCurrentProjectPosition);

		switch (mMenu[position].text) {
		case R.string.menu_issues:
			intent = new Intent(act, IssuesActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_projects:
			intent = new Intent(act, ProjectsActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_roadmap:
			intent = new Intent(act, RoadmapActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_wiki:
			intent = new Intent(act, WikiActivity.class);
			intent.putExtras(args);
			break;

		case R.string.menu_about:
			intent = new Intent(act, AboutActivity.class);
			break;

		case R.string.menu_settings:
			intent = new Intent(act, SettingsActivity.class);
			break;

		default:
			intent = new Intent(act, MainActivity.class);
			break;
		}

		startActivity(intent);
		act.toggle();
	}
}
