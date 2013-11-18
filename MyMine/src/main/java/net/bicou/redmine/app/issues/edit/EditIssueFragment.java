package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;

import net.bicou.redmine.Constants;
import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.app.issues.IssueFragment;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.IssueCategory;
import net.bicou.redmine.data.json.IssuePriority;
import net.bicou.redmine.data.json.IssueStatus;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Tracker;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.IssuePrioritiesDbAdapter;
import net.bicou.redmine.data.sqlite.IssueStatusesDbAdapter;
import net.bicou.redmine.data.sqlite.TrackersDbAdapter;
import net.bicou.redmine.data.sqlite.UsersDbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.util.BasicSpinnerAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;
import net.bicou.redmine.widget.DoneBarActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by bicou on 02/08/13.
 */
public class EditIssueFragment extends TrackedFragment {
	public static final String KEY_ISSUE_DESCRIPTION = "net.bicou.redmine.app.issues.edit.Description";
	public static final String KEY_ISSUE_NOTES = "net.bicou.redmine.app.issues.edit.Notes";
	public static final String KEY_ISSUE_PROJECT_ID = "net.bicou.redmine.app.issues.edit.Project";

	ViewGroup mMainLayout;
	Issue mIssue;
	Spinner mCategories, mVersions, mPriorities, mStatuses, mTrackers;
	EditText mEstimatedHours, mNotes;
	TextView mId, mAuthorName, mAssigneeName, mParentIssue, mSubject, mStartDate, mDueDate;
	ImageView mAuthorAvatar, mAssigneeAvatar;
	SeekBar mPercentDone;
	View mNotesLabel, mNotesLine, mNotesContainer;
	private boolean mIsCreationMode;
	private static java.text.DateFormat mLongDateFormat;
	private static java.text.DateFormat mTimeFormat;

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
		mMainLayout = (ViewGroup) v.findViewById(R.id.issue_edit_main_layout);

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

		// Used to hide the notes, if it's an issue creation (vs modification)
		mNotesLabel = v.findViewById(R.id.issue_edit_notes_label);
		mNotesLine = v.findViewById(R.id.issue_edit_notes_line);
		mNotesContainer = v.findViewById(R.id.issue_edit_notes_container);

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
		mIsCreationMode = mIssue == null || TextUtils.isEmpty(json);
		L.d("mIsCreationMode=" + mIsCreationMode);
		if (mIsCreationMode) {
			Server server = getArguments().getParcelable(Constants.KEY_SERVER);
			Project project = getArguments().getParcelable(Constants.KEY_PROJECT);
			mIssue = new Issue(server, project);

			// Hide avatar
			mAuthorAvatar.setVisibility(View.GONE);
			mAuthorName.setVisibility(View.GONE);
			mParentIssue.setVisibility(View.GONE);
		}

		AsyncTaskFragment.runTask((ActionBarActivity) getActivity(), EditIssueActivity.ACTION_LOAD_ISSUE_DATA, mIssue);

		DoneBarActivity.setupActionBar((ActionBarActivity) getActivity(), new DoneBarActivity.OnSaveActionListener() {
			@Override
			public void onSave() {
				saveIssueChangesAndClose();
			}
		});

		setHasOptionsMenu(true);

		return v;
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		mLongDateFormat = DateFormat.getLongDateFormat(activity);
		mTimeFormat = DateFormat.getTimeFormat(activity);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		L.d("");
		saveIssueChanges(true);
		outState.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(mIssue, Issue.class));
	}

	/**
	 * Updates the {@link #mIssue} object with the values from the form widgets
	 *
	 * @param isSilent If true, this method will not bug the user, otherwise it may display a Crouton if the form is not properly filled
	 * @return true if the form could be validated, false if there is an input error
	 * @throws java.lang.IllegalArgumentException when the form couldn't be parsed
	 */
	private boolean saveIssueChanges(boolean isSilent) {
		// Spinners, 'assigned to', description, target/start dates are automatically saved into #mIssue when the widgets are modified by the user
		// So, let's get the remaining data from the form
		mIssue.done_ratio = 10 * mPercentDone.getProgress();
		mIssue.subject = String.valueOf(mSubject.getText());

		// Tricky one: estimated hours
		Pattern hms = Pattern.compile("^(?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-5]?\\d)$");//([0-9]{1,2})?:?([0-9]{1,2})?:?([0-9]{1,2})");
		Matcher matcher = hms.matcher(mEstimatedHours.getText());
		try {
			if (matcher.find()) {
				int h, m, s;
				if (matcher.group(1) == null && matcher.group(2) == null) {
					h = Integer.parseInt(matcher.group(3));
					m = 0;
					s = 0;
				} else if (matcher.group(1) == null) {
					h = Integer.parseInt(matcher.group(2));
					m = Integer.parseInt(matcher.group(3));
					s = 0;
				} else {
					h = Integer.parseInt(matcher.group(1));
					m = Integer.parseInt(matcher.group(2));
					s = Integer.parseInt(matcher.group(3));
				}
				if (m > 60 || s > 60 || h < 0 || m < 0 || s < 0) {
					if (!isSilent) {
						throw new IllegalArgumentException("This message will never, ever be seen");
					}
				}
				mIssue.estimated_hours = h + ((double) m) / 60 + ((double) s) / 3600;
			} else {
				if (!isSilent) {
					throw new IllegalArgumentException("This message will never, ever be seen");
				}
			}
		} catch (Exception e) {
			if (!isSilent) {
				Crouton.makeText(getActivity(), getString(R.string.issue_edit_estimated_hours_parse_error), Style.ALERT, mMainLayout).show();
			}
			return false;
		}

		return true;
	}

	/**
	 * Triggered when the user chooses to commit the changes made to the form (create or edit issue)
	 */
	private void saveIssueChangesAndClose() {
		if (!saveIssueChanges(false)) {
			return;
		}

		// Notes are not part of the issue but only logged with an edit
		String notes = (mNotes == null || TextUtils.isEmpty(mNotes.getText())) ? "" : mNotes.getText().toString();

		Bundle taskParams = new Bundle();
		taskParams.putString(IssueFragment.KEY_ISSUE_JSON, new Gson().toJson(mIssue, Issue.class));
		taskParams.putString(EditIssueFragment.KEY_ISSUE_NOTES, notes);
		Intent result = new Intent();
		result.putExtras(taskParams);

		// If this line is reached, it means the form has been validated and we can try to upload this to the server
		getActivity().setResult(Activity.RESULT_OK, result);
	}

	/**
	 * Background task that will load all versions, priorities, statuses and trackers for issues; and their corresponding ID that matches the actual values from the
	 * {@link #mIssue} object.
	 *
	 * @param context Used to open the databases
	 * @param issue   Used to calculate the positions in the arrays of versions, priorities, statuses and trackers
	 * @return An object containing all the information required to fill the spinners and correctly select the appropriate item
	 */
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

		// Can't have nulls, have empty arrays instead
		if (info.categories == null) {
			info.categories = new ArrayList<IssueCategory>();
		}
		if (info.versions == null) {
			info.versions = new ArrayList<Version>();
		}
		if (info.priorities == null) {
			info.priorities = new ArrayList<IssuePriority>();
		}
		if (info.statuses == null) {
			info.statuses = new ArrayList<IssueStatus>();
		}
		if (info.trackers == null) {
			info.trackers = new ArrayList<Tracker>();
		}

		// Try to find currently selected items
		for (int i = 0; info.categories != null && i < info.categories.size() && issue.category != null; i++) {
			category = info.categories.get(i);
			if (category != null && category.id == issue.category.id) {
				info.category = i;
				break;
			}
		}

		for (int i = 0; info.priorities != null && i < info.priorities.size() && issue.priority != null; i++) {
			priority = info.priorities.get(i);
			if (priority != null && priority.id == issue.priority.id) {
				info.priority = i;
				break;
			}
		}

		for (int i = 0; info.versions != null && i < info.versions.size() && issue.fixed_version != null; i++) {
			version = info.versions.get(i);
			if (version != null && version.id == issue.fixed_version.id) {
				info.version = i;
				break;
			}
		}

		for (int i = 0; info.statuses != null && i < info.statuses.size() && issue.status != null; i++) {
			status = info.statuses.get(i);
			if (status != null && status.id == issue.status.id) {
				info.status = i;
				break;
			}
		}

		for (int i = 0; info.trackers != null && i < info.trackers.size() && issue.tracker != null; i++) {
			tracker = info.trackers.get(i);
			if (tracker != null && tracker.id == issue.tracker.id) {
				info.tracker = i;
				break;
			}
		}

		db.close();

		return info;
	}

	/**
	 * Create an array with at least one null item at the first position, and then all items contained in the source array, if any. This is used to be able to not
	 * choose a value from a spinner (for example: no specific target version)
	 *
	 * @param sourceArray The remaining items
	 * @return The array with null + all the remaining items from the source array
	 */
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

	/**
	 * Set selected spinner positions from the indices indicated into the {@link net.bicou.redmine.app.issues.edit.EditIssueFragment.IssueEditInformation}
	 *
	 * @param info Actual spinner position IDs corresponding to issue parameters
	 */
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

	/**
	 * Setup UI widgets so that their contents reflects the values in the current object {@link #mIssue}
	 */
	private void refreshUI() {
		if (mIssue.author != null && mIssue.author.id > 0) {
			mIssue.author = displayNameAndAvatar(getActivity(), mIssue, mAuthorName, mAuthorAvatar, mIssue.author, getString(R.string.issue_author_name_format), mIssue.created_on);
		} else {
			mAuthorName.setText("");
			mAuthorAvatar.setVisibility(View.INVISIBLE);
		}

		if (mIssue.assigned_to != null && mIssue.assigned_to.id > 0) {
			mIssue.assigned_to = displayNameAndAvatar(getActivity(), mIssue, mAssigneeName, mAssigneeAvatar, mIssue.assigned_to, mIssue.assigned_to.getName(), null);
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

		int h = (int) Math.floor(mIssue.estimated_hours);
		int m = (int) Math.floor((mIssue.estimated_hours - h) * 60);
		int s = (int) Math.floor(((mIssue.estimated_hours - h) * 60 - m) * 60);
		if (s > 0) {
			mEstimatedHours.setText(String.format(Locale.ENGLISH, "%d:%02d:%02d", h, m, s));
		} else if (m > 0) {
			mEstimatedHours.setText(String.format(Locale.ENGLISH, "%d:%02d", h, m));
		} else {
			mEstimatedHours.setText(String.format(Locale.ENGLISH, "%d", h));
		}

		// In create mode, there's no notes
		mNotesLabel.setVisibility(mIsCreationMode ? View.GONE : View.VISIBLE);
		mNotesLine.setVisibility(mIsCreationMode ? View.GONE : View.VISIBLE);
		mNotesContainer.setVisibility(mIsCreationMode ? View.GONE : View.VISIBLE);
	}

	private static View.OnClickListener mDatePopupClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View view) {
			Object tag = view.getTag();
			if (tag != null && tag instanceof Calendar) {
				Date date = ((Calendar) tag).getTime();
				String fullDate = mLongDateFormat.format(date) + " " + mTimeFormat.format(date);
				Toast.makeText(view.getContext(), fullDate, Toast.LENGTH_LONG).show();
			}
		}
	};

	public static User displayNameAndAvatar(Context context, Issue issue, TextView name, ImageView avatar, User user, String textResId, Calendar date) {
		if (user == null || user.id <= 0) {
			if (name != null) {
				name.setText("");
			}
			if (avatar != null) {
				avatar.setVisibility(View.INVISIBLE);
			}
			return null;
		}

		UsersDbAdapter db = new UsersDbAdapter(context);
		db.open();
		User u = db.select(issue.server, user.id);
		db.close();

		if (u != null) {
			if (u.createGravatarUrl() && avatar != null) {
				ImageLoader.getInstance().displayImage(u.gravatarUrl, avatar);
				avatar.setVisibility(View.VISIBLE);
			} else if (avatar != null) {
				avatar.setVisibility(View.INVISIBLE);
			}

			if (name != null) {
				name.setText(String.format(textResId, u.getName(), Util.getDeltaDateText(context, date)));
				name.setOnClickListener(mDatePopupClickListener);
				name.setTag(date);
			}
		}

		return u;
	}

	public void showDatePickerDialog(final View v) {
		saveIssueChanges(true);
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
		saveIssueChanges(true);
		Bundle args = new Bundle();
		args.putString(KEY_ISSUE_DESCRIPTION, mIssue.description);
		DialogFragment newFragment = DescriptionEditorFragment.newInstance(args);
		newFragment.show(getActivity().getSupportFragmentManager(), "descriptionEditor");
	}

	private void showEditProjectDialog() {
		saveIssueChanges(true);
		Bundle args = new Bundle();
		args.putLong(KEY_ISSUE_PROJECT_ID, mIssue.project == null ? 0 : mIssue.project.id);
		DialogFragment newFragment = ProjectSwitcherFragment.newInstance(args);
		newFragment.show(getActivity().getSupportFragmentManager(), "projectSwitcher");
	}

	public void onDescriptionChanged(final String newDescription) {
		mIssue.description = newDescription;
		refreshUI();
	}

	public void onProjectChanged(long newProjectId) {
		mIssue.project = new Project();
		mIssue.project.id = newProjectId;
	}

	private void showUserPickerDialog() {
		saveIssueChanges(true);
		Bundle args = new Bundle();
		String userJson = mIssue.assigned_to == null || mIssue.assigned_to.id <= 0 ? "" : new Gson().toJson(mIssue.assigned_to, User.class);
		args.putString(UserPickerFragment.KEY_USER, userJson);
		args.putLong(Constants.KEY_SERVER_ID, mIssue.server == null ? 0 : mIssue.server.rowId);

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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_issue_edit_change_project:
			showEditProjectDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
