package net.bicou.redmine.data.json;

import java.util.List;

public class ProjectMember {
	public int id;
	public Reference project;
	public Reference user;
	public List<Reference> roles;

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " { id: " + id + ", project: " + project + ", user: " + user + " }";
	}
}
