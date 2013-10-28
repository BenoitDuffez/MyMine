package net.bicou.redmine.app.issues.edit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;

import net.bicou.redmine.R;
import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.json.Issue;
import net.bicou.redmine.data.json.User;

import java.util.Calendar;

/**
 * Created by bicou on 02/08/13.
 */
public class EditIssueActivity extends ActionBarActivity implements AsyncTaskFragment.TaskFragmentCallbacks, UserPickerDialog.OnUserSelectedListener,
        DatePickerFragment.DateSelectionListener, DescriptionEditorFragment.DescriptionChangeListener {
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
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, content).commit();
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
            EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
            frag.setupSpinners((EditIssueFragment.IssueEditInformation) result);
        }
    }

    @Override
    public void onUserSelected(final User user) {
        EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        frag.onAssigneeChosen(user);
    }

    @Override
    public void onDateSelected(final int id, final Calendar calendar) {
        EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        frag.onDatePicked(id, calendar);
    }

    @Override
    public void onDescriptionChanged(final String newDescription) {
        EditIssueFragment frag = (EditIssueFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        frag.onDescriptionChanged(newDescription);
    }
}
