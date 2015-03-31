package net.bicou.redmine.app;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

public abstract class SeparatorSpinnerAdapter implements SpinnerAdapter {
	Context mContext;
	List<Object> mData;
	int mSeparatorLayoutResId, mActionBarItemLayoutResId, mDropDownItemLayoutResId, mTextViewResId;

	public static class SpinnerSeparator {
		public int separatorTextResId;

		public SpinnerSeparator(final int resId) {
			separatorTextResId = resId;
		}
	}

	public abstract String getText(int position);

	public SeparatorSpinnerAdapter(final Context ctx, final List<Object> data, final int separatorLayoutResId, final int actionBarItemLayoutResId,
			final int dropDownItemLayoutResId, final int textViewResId) {
		mContext = ctx;
		mData = data;
		mSeparatorLayoutResId = separatorLayoutResId;
		mActionBarItemLayoutResId = actionBarItemLayoutResId;
		mDropDownItemLayoutResId = dropDownItemLayoutResId;
		mTextViewResId = textViewResId;
	}

	protected String getString(final int resId) {
		return mContext.getString(resId);
	}

	@Override
	public void registerDataSetObserver(final DataSetObserver observer) {
	}

	@Override
	public void unregisterDataSetObserver(final DataSetObserver observer) {
	}

	@Override
	public int getCount() {
		if (mData != null) {
			return mData.size();
		}
		return 0;
	}

	@Override
	public Object getItem(final int position) {
		return mData == null ? null : mData.get(position);
	}

	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	@Override
	public long getItemId(final int position) {
		return 0;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		return getView(mActionBarItemLayoutResId, position, convertView, parent);
	}

	public boolean isSeparator(final int position) {
		final Object item = getItem(position);
		if (item != null) {
			return item instanceof SpinnerSeparator;
		}
		return false;
	}

	@Override
	public int getItemViewType(final int position) {
		return isSeparator(position) ? 0 : 1;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
		return getView(isSeparator(position) ? mSeparatorLayoutResId : mDropDownItemLayoutResId, position, convertView, parent);
	}

	private View getView(final int layoutResId, final int position, final View convertView, final ViewGroup parent) {
		View v;

		if (convertView == null || (Integer) convertView.getTag() != getItemViewType(position)) {
			final LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = li.inflate(layoutResId, parent, false);
		} else {
			v = convertView;
		}
		v.setTag(Integer.valueOf(getItemViewType(position)));

		final TextView tv = (TextView) v.findViewById(mTextViewResId);
		if (tv != null) {
			tv.setText(getText(position));

			if (isSeparator(position)) {
				tv.setOnClickListener(null);
				tv.setOnTouchListener(null);
			}
		}

		return v;
	}
}
