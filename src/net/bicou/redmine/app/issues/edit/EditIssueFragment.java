package net.bicou.redmine.app.issues.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.google.gson.Gson;
import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.issues.IssueFragment;
import net.bicou.redmine.app.issues.IssueOverviewFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.*;
import net.bicou.redmine.data.sqlite.*;
import net.bicou.redmine.util.BasicSpinnerAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;
import net.bicou.redmine.widget.CancelSaveActionBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by bicou on 02/08/13.
 */
public class EditIssueFragment extends SherlockFragment {
	public static final String KEY_ISSUE_DESCRIPTION = "net.bicou.redmine.app.issues.edit.Description";

	Issue mIssue;
	Spinner mCategories, mVersions, mPriorities, mStatuses, mTrackers;
	EditText mEstimatedHours, mNotes;
	TextView mId, mAuthorName, mAssigneeName, mParentIssue, mSubject, mStartDate, mDueDate;
	ImageView mAuthorAvatar, mAssigneeAvatar;
	SeekBar mPercentDone;

	public static class IssueEditInformation {
		public ArrayList<IssueStatus> statuses;
		public ArrayList<IssueCategory> categories;
		public ArrayList<IssuePriority> priorities;
		public ArrayList<Version> versions;
		public ArrayList<Tracker> trackers;
		public int category, status, priority, version, tracker; // position in spinner/array
	}

	public static EditIssueFragment newInstance(Bundle args) {
		EditIssueFragment frag = new EditIssueFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_issue_edit, container, false);

		// Labels
		mId = (TextView) v.findViewById(R.id.issue_edit_id);
		mAuthorName = (TextView) v.findViewById(R.id.issue_edit_author);
		mAssigneeName = (TextView) v.findViewById(R.id.issue_edit_assignee_name);
		mParentIssue = (TextView) v.findViewById(R.id.issue_edit_parent);
		mSubject = (TextView) v.findViewById(R.id.issue_edit_subject);
		mStartDate = (TextView) v.findViewById(R.id.issue_edit_start_date);
		mDueDate = (TextView) v.findViewById(R.id.issue_edit_due_date);
		mAuthorAvatar = (ImageView) v.findViewById(R.id.issue_edit_author_avatar);
		mAssigneeAvatar = (ImageView) v.findViewById(R.id.issue_edit_assignee_avatar);

		// Spinners
		mCategories = (Spinner) v.findViewById(R.id.issue_edit_category);
		mVersions = (Spinner) v.findViewById(R.id.issue_edit_target_version);
		mPriorities = (Spinner) v.findViewById(R.id.issue_edit_priority);
		mStatuses = (Spinner) v.findViewById(R.id.issue_edit_status);
		mTrackers = (Spinner) v.findViewById(R.id.issue_edit_tracker);

		mPercentDone = (SeekBar) v.findViewById(R.id.issue_edit_percent_done);

		// Editable fields
		mEstimatedHours = (EditText) v.findViewById(R.id.issue_edit_estimated_hours);
		mNotes = (EditText) v.findViewById(R.id.issue_edit_notes);

		View.OnClickListener datePicker = new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				showDatePickerDialog(view);
			}
		};
		mStartDate.setOnClickListener(datePicker);
		mDueDate.setOnClickListener(datePicker);

		v.findViewById(R.id.issue_edit_change_description).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				showEditDescriptionDialog(view);
			}
		});

		v.findViewById(R.id.issue_edit_assignee_picker).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				showUserPickerDialog();
			}
		});

		String json;
		if (savedInstanceState == null) {
			json = getArguments().getString(IssueFragment.KEY_ISSUE_JSON);
		} else if (getArguments() != null && getArguments().containsKey(IssueFragment.KEY_ISSUE_JSON)) {
			json = savedInstanceState.getString(IssueFragment.KEY_ISSUE_JSON);
		} else {
			json = null;
		}

		mIssue = new Gson().fromJson(json, Issue.class);

		// Not edit, but create
		if (mIssue == null) {
			Server server = getArguments().getParcelable(Constants.KEY_SERVER);
			Project project = getArguments().getParcelable(Constants.KEY_PROJECT);
			mIssue = new Issue(server, project);
		}

		AsyncTaskFragment.runTask(getSherlockActivity(), EditIssueActivity.ACTION_LOAD_ISSUE_DATA, mIssue);

		CancelSaveActionBar.setupActionBar(getSherlockActivity(), new CancelSaveActionBar.CancelSaveActionBarCallbacks() {
			@Override
			public void onSave() {
				saveIssueChangesAndClose();
			}
		});

		return v;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(mIssue, Issue.class));
	}

	private void saveIssueChangesAndClose() {
		mIssue.done_ratio = 10 * mPercentDone.getProgress();
		mIssue.subject = String.valueOf(mSubject.getText());
		String notes = mNotes == null || mNotes.getText() == null ? "" : mNotes.getText().toString();
		Object[] taskParams = new Object[] {
				mIssue,
				notes,
		};
		AsyncTaskFragment.runTask(getSherlockActivity(), EditIssueActivity.ACTION_UPLOAD_ISSUE, taskParams);
	}

	@SuppressWarnings("unchecked")
	public static IssueEditInformation loadSpinnersData(final Context context, Issue issue) {
		if (issue == null || issue.server == null || issue.project == null || issue.server.rowId <= 0 || issue.project.id <= 0) {
			throw new IllegalStateException("Invalid issue: " + issue);
		}

		IssueCategoriesDbAdapter db = new IssueCategoriesDbAdapter(context);
		db.open();
		VersionsDbAdapter vdb = new VersionsDbAdapter(db);
		IssuePrioritiesDbAdapter pdb = new IssuePrioritiesDbAdapter(db);
		IssueStatusesDbAdapter sdb = new IssueStatusesDbAdapter(db);
		TrackersDbAdapter tdb = new TrackersDbAdapter(db);

		IssueEditInformation info = new IssueEditInformation();
		info.categories = (ArrayList<IssueCategory>) getArrayWithDummy(db.selectAll(issue.server, issue.project));
		info.versions = (ArrayList<Version>) getArrayWithDummy(vdb.selectAllOpen(issue.server, issue.project));
		info.priorities = pdb.selectAll(issue.server);
		info.statuses = sdb.selectAll(issue.server);
		info.trackers = tdb.selectAll(issue.server);

		// Used in the loops to avoid GC
		Version version;
		Tracker tracker;
		IssueStatus status;
		IssueCategory category;
		IssuePriority priority;

		// Try to find currently selected items
		for (int i = 0; i < info.categories.size() && issue.category != null; i++) {
			category = info.categories.get(i);
			if (category != null && category.id == issue.category.id) {
				info.category = i;
				break;
			}
		}

		for (int i = 0; i < info.priorities.size() && issue.priority != null; i++) {
			priority = info.priorities.get(i);
			if (priority != null && priority.id == issue.priority.id) {
				info.priority = i;
				break;
			}
		}

		for (int i = 0; i < info.versions.size() && issue.fixed_version != null; i++) {
			version = info.versions.get(i);
			if (version != null && version.id == issue.fixed_version.id) {
				info.version = i;
				break;
			}
		}

		for (int i = 0; i < info.statuses.size() && issue.status != null; i++) {
			status = info.statuses.get(i);
			if (status != null && status.id == issue.status.id) {
				info.status = i;
				break;
			}
		}

		for (int i = 0; i < info.trackers.size() && issue.tracker != null; i++) {
			tracker = info.trackers.get(i);
			if (tracker != null && tracker.id == issue.tracker.id) {
				info.tracker = i;
				break;
			}
		}

		db.close();

		return info;
	}

	private static ArrayList<?> getArrayWithDummy(List<?> sourceArray) {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(null);
		if (sourceArray != null && sourceArray.size() > 0) {
			for (Object o : sourceArray) {
				list.add(o);
			}
		}
		return list;
	}

	public void setupSpinners(IssueEditInformation info) {
		if (info != null) {
			mCategories.setAdapter(new BasicSpinnerAdapter<IssueCategory>(getActivity(), info.categories) {
				@Override
				public String getText(final IssueCategory item) {
					return item.name;
				}
			});
			mCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
					mIssue.category = (IssueCategory) parent.getAdapter().getItem(position);
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parent) {
				}
			});
			mCategories.setSelection(info.category);

			mPriorities.setAdapter(new BasicSpinnerAdapter<IssuePriority>(getActivity(), info.priorities) {
				@Override
				public String getText(final IssuePriority item) {
					return item.name;
				}
			});
			mPriorities.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
					mIssue.priority = (IssuePriority) parent.getAdapter().getItem(position);
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parent) {
				}
			});
			mPriorities.setSelection(info.priority);

			mVersions.setAdapter(new BasicSpinnerAdapter<Version>(getActivity(), info.versions) {
				@Override
				public String getText(final Version item) {
					return item.name;
				}
			});
			mVersions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
					mIssue.fixed_version = (Version) parent.getAdapter().getItem(position);
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parent) {
				}
			});
			mVersions.setSelection(info.version);

			mStatuses.setAdapter(new BasicSpinnerAdapter<IssueStatus>(getActivity(), info.statuses) {
				@Override
				public String getText(final IssueStatus item) {
					return item.name;
				}
			});
			mStatuses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
					mIssue.status = (IssueStatus) parent.getAdapter().getItem(position);
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parent) {
				}
			});
			mStatuses.setSelection(info.status);

			mTrackers.setAdapter(new BasicSpinnerAdapter<Tracker>(getActivity(), info.trackers) {
				@Override
				public String getText(final Tracker item) {
					return item.name;
				}
			});
			mTrackers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
					mIssue.tracker = (Tracker) parent.getAdapter().getItem(position);
				}

				@Override
				public void onNothingSelected(final AdapterView<?> parent) {
				}
			});
			mTrackers.setSelection(info.tracker);
		}

		refreshUI();
	}

	private void refreshUI() {
		if (mIssue.author != null && mIssue.author.id > 0) {
			mIssue.author = IssueOverviewFragment.displayNameAndAvatar(getActivity(), mIssue, mAuthorName, mAuthorAvatar, mIssue.author,
					getString(R.string.issue_author_name_format), mIssue.created_on);
		} else {
			mAuthorName.setText("");
			mAuthorAvatar.setVisibility(View.INVISIBLE);
		}

		if (mIssue.assigned_to != null && mIssue.assigned_to.id > 0) {
			mIssue.assigned_to = IssueOverviewFragment.displayNameAndAvatar(getActivity(), mIssue, mAssigneeName, mAssigneeAvatar, mIssue.assigned_to,
					mIssue.assigned_to.getName(), null);
		} else {
			mAssigneeAvatar.setVisibility(View.INVISIBLE);
			mAssigneeName.setText(getString(R.string.issue_edit_field_unset));
		}

		mEstimatedHours.setText(String.format("%.2f", mIssue.spent_hours));
		if (mIssue.id > 0) {
			mId.setText(String.format("#%d", mIssue.id));
		} else {
			mId.setText("");
		}
		mParentIssue.setText(mIssue.parent != null && mIssue.parent.id > 0 ? Long.toString(mIssue.parent.id) : "");
		java.text.DateFormat format = DateFormat.getMediumDateFormat(getActivity());
		mSubject.setText(mIssue.subject != null ? mIssue.subject : "");
		mStartDate.setText(Util.isEpoch(mIssue.start_date) ? getString(R.string.issue_edit_field_unset) : format.format(mIssue.start_date.getTime()));
		mDueDate.setText(Util.isEpoch(mIssue.due_date) ? getString(R.string.issue_edit_field_unset) : format.format(mIssue.due_date.getTime()));
		mPercentDone.setProgress((int) (mIssue.done_ratio / 10.0));
	}

	public void showDatePickerDialog(final View v) {
		Bundle args = new Bundle();
		Calendar cal = new GregorianCalendar();
		switch (v.getId()) {
		case R.id.issue_edit_due_date:
			if (!Util.isEpoch(mIssue.due_date)) {
				cal.setTimeInMillis(mIssue.due_date.getTimeInMillis());
			}
			break;

		case R.id.issue_edit_start_date:
			if (!Util.isEpoch(mIssue.start_date)) {
				cal.setTimeInMillis(mIssue.start_date.getTimeInMillis());
			}
			break;
		}
		args.putInt(DatePickerFragment.KEY_REQUEST_ID, v.getId());
		args.putLong(DatePickerFragment.KEY_DEFAULT_DATE, cal.getTimeInMillis());

		DialogFragment newFragment = DatePickerFragment.newInstance(args);
		newFragment.setArguments(args);
		newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
	}

	public void onDatePicked(final int id, final Calendar calendar) {
		switch (id) {
		case R.id.issue_edit_due_date:
			mIssue.due_date.setTimeInMillis(calendar.getTimeInMillis());
			break;

		case R.id.issue_edit_start_date:
			mIssue.start_date.setTimeInMillis(calendar.getTimeInMillis());
			break;
		}

		refreshUI();
	}

	private void showEditDescriptionDialog(final View view) {
		Bundle args = new Bundle();
		args.putString(KEY_ISSUE_DESCRIPTION, mIssue.description);
		DialogFragment newFragment = DescriptionEditorFragment.newInstance(args);
		newFragment.show(getActivity().getSupportFragmentManager(), "descriptionEditor");
	}

	public void onDescriptionChanged(final String newDescription) {
		mIssue.description = newDescription;
		refreshUI();
	}

	private void showUserPickerDialog() {
		Bundle args = new Bundle();
		String userJson = mIssue.assigned_to == null || mIssue.assigned_to.id <= 0 ? "" : new Gson().toJson(mIssue.assigned_to, User.class);
		args.putString(UserPickerFragment.KEY_USER, userJson);

		DialogFragment newFragment = UserPickerFragment.newInstance(args);
		newFragment.show(getActivity().getSupportFragmentManager(), "userPicker");
	}

	public void onAssigneeChosen(final User user) {
		L.d("Assignee was: " + mIssue.assigned_to + "; it's now: " + user);
		mIssue.assigned_to = user;
		refreshUI();
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_issue_edit, menu);
	}
}
