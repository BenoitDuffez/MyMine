package net.bicou.redmine.app.issues.edit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.User;
import net.bicou.redmine.util.Util;

import java.util.Calendar;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by bicou on 02/08/13.
 */
public class EditIssueActivity extends ActionBarActivity implements AsyncTaskFragment.TaskFragmentCallbacks, UserPickerDialog.OnUserSelectedListener, DatePickerFragment.DateSelectionListener, DescriptionEditorFragment.DescriptionChangeListener, ProjectSwitcherFragment.ProjectChangeListener {
	public static final int ACTION_LOAD_ISSUE_DATA = 0;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_PROGRESS);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.fragment_activity);
		AsyncTaskFragment.attachAsyncTaskFragment(this);
		if (savedInstanceState == null) {
			Fragment content = EditIssueFragment.newInstance(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(Util.getContentViewCompat(), content).commit();
		}

		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onBackPressed() {
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			EditIssueFragment eif = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
			eif.saveIssueChangesAndClose(Activity.RESULT_CANCELED);
		}
		super.onBackPressed();
	}

	public static void handleRevertCrouton(final Activity activity, int croutonHolder, final int requestCode, final Bundle params) {
		if (params == null) {
			return;
		}

		View croutonView = LayoutInflater.from(activity).inflate(R.layout.crouton_revert, null);
		TextView msg = (TextView) croutonView.findViewById(R.id.crouton_message);
		msg.setText(R.string.issue_edit_revert_crouton);

		final View.OnClickListener revert = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(activity, EditIssueActivity.class);
				intent.putExtras(params);
				activity.startActivityForResult(intent, requestCode);
			}
		};

		Crouton crouton = Crouton.make(activity, croutonView, croutonHolder);
		croutonView.findViewById(R.id.crouton_layout).setOnClickListener(revert);
		crouton.setOnClickListener(revert);

		Configuration config = new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build();
		crouton.setConfiguration(config);
		crouton.show();
	}

	@Override
	public void onPreExecute(final int action, final Object parameters) {
		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public Object doInBackGround(final Context applicationContext, final int action, final Object parameters) {
		if (action == ACTION_LOAD_ISSUE_DATA) {
			return EditIssueFragment.loadSpinnersData(applicationContext, (Issue) parameters);
		}
		return null;
	}

	@Override
	public void onPostExecute(final int action, final Object parameters, final Object result) {
		setSupportProgressBarIndeterminateVisibility(false);
		if (action == ACTION_LOAD_ISSUE_DATA) {
			EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
			frag.setupSpinners((EditIssueFragment.IssueEditInformation) result);

			if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(IssueUploader.KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON)) {
				Crouton.makeText(this, getIntent().getExtras().getString(IssueUploader.KEY_SHOW_ISSUE_UPLOAD_ERROR_CROUTON), Style.ALERT).show();
			}
		}
	}

	@Override
	public void onUserSelected(final User user) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onAssigneeChosen(user);
	}

	@Override
	public void onDateSelected(final int id, final Calendar calendar) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onDatePicked(id, calendar);
	}

	@Override
	public void onDescriptionChanged(final String newDescription) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onDescriptionChanged(newDescription);
	}

	@Override
	public void onProjectChanged(long newProjectId) {
		EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(Util.getContentViewCompat());
		frag.onProjectChanged(newProjectId);
	}
}
