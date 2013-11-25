package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

public class UsersList extends AbsObjectList<User> {
	public List<User> users;

	@Override
	public void addObjects(List<User> objects) {
		if (users == null) {
			users = new ArrayList<User>();
		}
		users.addAll(objects);
	}

	@Override
	public List<User> getObjects() {
		return users;
	}

	@Override
	public int getSize() {
		return users == null ? 0 : users.size();
	}
}
