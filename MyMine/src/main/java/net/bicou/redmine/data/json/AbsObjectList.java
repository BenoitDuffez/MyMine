package net.bicou.redmine.data.json;

import java.util.List;

public abstract class AbsObjectList<T> {
	public int total_count;
	public int limit;
	public int offset;

	public int downloadedObjects;

	public abstract void addObjects(List<T> objects);

	public abstract List<T> getObjects();

	public abstract int getSize();

	public void init(final AbsObjectList<T> from) {
		total_count = from.total_count;
		limit = from.limit;
		offset = from.offset;
		downloadedObjects = from.downloadedObjects;
	}
}
