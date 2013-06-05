package net.bicou.redmine.data.json;


public class Reference {
	public String name;
	public long id;

	public static final String KEY_NAME = "name";

	public Reference() {

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " { id: " + id + ", name: " + name + " }";
	}
}
