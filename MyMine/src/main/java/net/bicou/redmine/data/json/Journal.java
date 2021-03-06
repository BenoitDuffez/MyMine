package net.bicou.redmine.data.json;

import android.text.SpannableStringBuilder;

import java.util.Calendar;
import java.util.List;

public class Journal {
	public long id;
	public User user;
	public String notes;
	public Calendar created_on;
	public List<JournalDetail> details;

	// These are not from the JSON/Redmine server
	public SpannableStringBuilder formatted_details;
	public SpannableStringBuilder formatted_notes;
}
