package net.bicou.redmine.data.json;

import java.util.Calendar;

/**
 * Created by bicou on 21/05/13.
 */
public class ChangeSet {
	public String revision;
	public User user;
	public String comments;
	public Calendar committed_on;

	public String commentsHtml;
}
