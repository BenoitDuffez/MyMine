package net.bicou.redmine.data.json;

import java.util.ArrayList;
import java.util.List;

public class VersionsList extends AbsObjectList<Version> {
	public List<Version> versions;

	@Override
	public String toString() {
		return "{ total_count: " + total_count + ", versions: " + versions + " }";
	}

	@Override
	public void addObjects(final List<Version> objects) {
		if (versions == null) {
			versions = new ArrayList<Version>();
		}
		versions.addAll(objects);
	}

	@Override
	public List<Version> getObjects() {
		return versions;
	}

	@Override
	public int getSize() {
		return versions == null ? 0 : versions.size();
	}
}
