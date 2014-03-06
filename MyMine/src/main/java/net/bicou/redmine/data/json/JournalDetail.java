package net.bicou.redmine.data.json;

/**
 * Describes a property change from the issue history
 */
public class JournalDetail {
	/**
	 * The changed property type (attribute, attachment, custom field, etc.)
	 */
	public String property;

	/**
	 * The changed property name
	 */
	public String name;

	/**
	 * The previous property value (usually, an ID)
	 */
	public String old_value;

	/**
	 * The previous property value (usually, an ID)
	 */
	public String new_value;
}
