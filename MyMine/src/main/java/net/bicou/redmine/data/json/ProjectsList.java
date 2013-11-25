package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

public class ProjectsList extends AbsObjectList<Project> {
	public List<Project> projects;

	@Override
	public void addObjects(final List<Project> objects) {
		if (projects == null) {
			projects = new ArrayList<Project>();
		}
		projects.addAll(objects);
	}

	@Override
	public List<Project> getObjects() {
		return projects;
	}

	@Override
	public int getSize() {
		return projects == null ? 0 : projects.size();
	}
}
