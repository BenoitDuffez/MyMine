package net.bicou.redmine;

public class Constants {
	/**
	 * Account type string.
	 */
	public static final String ACCOUNT_TYPE = "net.bicou.redmine.sync";

	/**
	 * Authtoken type string.
	 */
	public static final String AUTHTOKEN_TYPE = "net.bicou.redmine.sync";

	public static final String KEY_PROJECT = "net.bicou.redmine.Project";
	public static final String KEY_PROJECT_ID = "net.bicou.redmine.ProjectID";
	public static final String KEY_PROJECT_POSITION = "net.bicou.redmine.ProjectPosition";
	public static final String KEY_ISSUE_ID = "net.bicou.redmine.IssueID";
	public static final String KEY_SERVER = "net.bicou.redmine.Server";
	public static final String KEY_SERVER_ID = "net.bicou.redmine.ServerID";

	/**
	 * How many days in the past should we sync the issues
	 */
	public static final int SYNC_ISSUES_PAST_DAYS = -180;

	/**
	 * How many issues we download at once (Redmine maximum is 100)
	 */
	public static final int ISSUES_LIST_BURST_DOWNLOAD_COUNT = 100;
}
