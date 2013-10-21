package net.bicou.redmine.app.issues.edit;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by bicou on 06/08/13.
 */
public class DatePickerFragment extends DialogFragment {
	public static final String KEY_DEFAULT_DATE = "net.bicou.redmine.app.issues.edit.DefaultDate";
	public static final String KEY_REQUEST_ID = "net.bicou.redmine.app.issues.edit.RequestId";
	Calendar mDefaultDate;

	public interface DateSelectionListener {
		public void onDateSelected(int id, Calendar calendar);
	}

	public static DatePickerFragment newInstance(Bundle args) {
		DatePickerFragment frag = new DatePickerFragment();
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker
		mDefaultDate = new GregorianCalendar();
		if (savedInstanceState == null) {
			mDefaultDate.setTimeInMillis(getArguments().getLong(KEY_DEFAULT_DATE));
		} else {
			mDefaultDate.setTimeInMillis(savedInstanceState.getLong(KEY_DEFAULT_DATE));
		}

		final int year = mDefaultDate.get(Calendar.YEAR);
		final int month = mDefaultDate.get(Calendar.MONTH);
		final int day = mDefaultDate.get(Calendar.DAY_OF_MONTH);

		// Create a new instance of DatePickerDialog and return it
		final DatePickerDialog dialog = new DatePickerDialog(getActivity(), null, year, month, day);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.cancel();
			}
		});
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialogInterface, final int which) {
				Calendar calendar = new GregorianCalendar();
				DatePicker datePicker = dialog.getDatePicker();
				calendar.set(Calendar.YEAR, datePicker.getYear());
				calendar.set(Calendar.MONTH, datePicker.getMonth());
				calendar.set(Calendar.DATE, datePicker.getDayOfMonth());

				if (getActivity() instanceof DateSelectionListener) {
					((DateSelectionListener) getActivity()).onDateSelected(getArguments().getInt(KEY_REQUEST_ID), calendar);
				}

				dialog.dismiss();
			}
		});
		return dialog;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_DEFAULT_DATE, mDefaultDate.getTimeInMillis());
	}
}
