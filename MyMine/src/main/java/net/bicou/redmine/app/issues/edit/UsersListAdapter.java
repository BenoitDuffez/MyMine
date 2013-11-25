package net.bicou.redmine.app.issues.edit;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import net.bicou.redmine.R;
import net.bicou.redmine.data.json.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 06/08/13.
 */
class UsersListAdapter extends BaseAdapter {
	List<User> mUsers;
	View mEmptyView;

	private static class ViewsHolder {
		ImageView avatar;
		TextView name;
	}

	public UsersListAdapter(final View emptyView) {
		mUsers = new ArrayList<User>();
		mEmptyView = emptyView;
	}

	public void setUsers(List<User> users) {
		mUsers.clear();
		mUsers.addAll(users);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if (mUsers.size() <= 0) {
			mEmptyView.setVisibility(View.VISIBLE);
		} else {
			mEmptyView.setVisibility(View.GONE);
		}
		return mUsers.size();
	}

	@Override
	public User getItem(final int position) {
		return position >= 0 && position < getCount() ? mUsers.get(position) : null;
	}

	@Override
	public long getItemId(final int position) {
		return 0;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view;
		ViewsHolder vh;
		if (convertView == null) {
			view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_list_item, parent, false);
			vh = new ViewsHolder();
			vh.avatar = (ImageView) view.findViewById(R.id.users_list_item_avatar);
			vh.name = (TextView) view.findViewById(R.id.users_list_item_name);
			view.setTag(vh);
		} else {
			view = convertView;
			vh = (ViewsHolder) view.getTag();
		}

		User user = getItem(position);
		if (user != null) {
			if (user.id > 0 && user.createGravatarUrl()) {
				vh.avatar.setVisibility(View.VISIBLE);
				ImageLoader.getInstance().displayImage(user.gravatarUrl, vh.avatar);
			} else {
				vh.avatar.setVisibility(View.INVISIBLE);
			}

			if (TextUtils.isEmpty(user.login)) {
				vh.name.setText(user.getName());
			} else {
				vh.name.setText(String.format("%s (%s)", user.getName(), user.login));
			}
		}

		return view;
	}
}
