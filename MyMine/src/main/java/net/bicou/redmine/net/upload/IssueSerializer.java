package net.bicou.redmine.net.upload;

import android.content.Context;
import android.text.TextUtils;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.sqlite.IssuesDbAdapter;

import java.util.HashMap;

/**
 * Created by bicou on 07/08/13.
 */
public class IssueSerializer extends ObjectSerializer<Issue> {
	String mNotes;

	public IssueSerializer(final Context context, Issue issue, String notes) {
		this(context, issue, notes, false);
	}

	public IssueSerializer(final Context context, Issue issue, String notes, boolean isDelete) {
		super(context, "issue", issue, isDelete);
		mNotes = notes;
		if (issue == null || issue.server == null || issue.server.rowId <= 0) {
			throw new IllegalArgumentException("Invalid issue modification parameters provided");
		}
	}

	@Override
	protected void saveAdditionalParameters(final HashMap<String, Object> fields) {
		if (!TextUtils.isEmpty(mNotes)) {
			fields.put("notes", mNotes);
		}
	}

	@Override
	protected Issue getOldObject() {
		IssuesDbAdapter db = new IssuesDbAdapter(mContext);
		db.open();
		Issue issue = db.select(mNewObject.server, mNewObject.id, null);
		db.close();
		return issue;
	}

	@Override
	protected String serializeField(Object item) {
		if (item instanceof FileUpload) {
			// FileUpload is not complicated, just dump the fields using Gson
			return new Gson().toJson(item);
		} else {
			return super.serializeField(item);
		}
	}

	@Override
	protected String[] getDefaultFields() {
		return new String[] {
				"project",
				"tracker",
				"status",
				"priority",
				"category",
				"parent",
				"fixed_version",
				"assigned_to",
				"author",

				"subject",
				"description",
				"start_date",
				"created_on",
				"due_date",
				"done_ratio",
				"estimated_hours",
				"spent_hours",
				"is_private",

				"uploads",

				//TODO
				//		public List<Journal> journals;
				//		public List<ChangeSet> changesets;
				//		public List<Attachment> attachments;
		};
	}
}
