package net.bicou.redmine.app.issues.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import net.bicou.redmine.R;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;

import java.util.List;

/**
 * Created by bicou on 06/08/13.
 */
class UserPickerDialog extends AlertDialog {
	User mUser;
	OnUserSelectedListener mListener;

	public interface OnUserSelectedListener {
		public void onUserSelected(User user);
	}

	UserPickerDialog(final Context context, User user, final long serverId, OnUserSelectedListener listener) {
		super(context);
		mUser = user;
		mListener = listener;

		View v = getLayoutInflater().inflate(R.layout.users_list, null);
		setView(v);
		setTitle(context.getString(R.string.users_picker_dialog_title));

		final UsersListAdapter adapter = new UsersListAdapter(v.findViewById(android.R.id.empty));
		ListView lv = (ListView) v.findViewById(android.R.id.list);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				if (mListener != null) {
					mListener.onUserSelected((User) parent.getAdapter().getItem(position));
				}
				dismiss();
			}
		});

		new AsyncTask<Void, Void, List<User>>() {
			@Override
			protected List<User> doInBackground(final Void... params) {
				UsersDbAdapter db = new UsersDbAdapter(getContext());
				db.open();
				List<User> users = db.selectAll(serverId);
				db.close();
				return users;
			}

			@Override
			protected void onPostExecute(final List<User> users) {
				adapter.setUsers(users);
			}
		}.execute();
	}

	public User getUser() {
		return mUser;
	}
}
