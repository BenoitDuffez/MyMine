package net.bicou.redmine.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.ImageView;

/**
 * Created by bicou on 03/11/2013.
 */
public class NoParentPressImageView extends ImageView implements Checkable {
	private boolean mChecked = false;
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
	private boolean mBroadcasting;
	private OnCheckedChangeListener mOnCheckedChangeListener;

	public NoParentPressImageView(Context context) {
		this(context, null);
	}

	public NoParentPressImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setPressed(boolean pressed) {
		// If the parent is pressed, do not set to pressed.
		if (pressed && getParent() != null && ((View) getParent()).isPressed()) {
			return;
		}
		super.setPressed(pressed);
	}

	@Override
	public int[] onCreateDrawableState(final int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked() && drawableState != null) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		invalidate();
	}

	@Override
	public boolean performClick() {
		toggle();
		return super.performClick();
	}

	@Override
	public void setChecked(boolean checked) {
		if (mChecked != checked) {
			mChecked = checked;
			refreshDrawableState();

			// Avoid infinite recursions if setChecked() is called from a listener
			if (mBroadcasting) {
				return;
			}

			mBroadcasting = true;
			if (mOnCheckedChangeListener != null) {
				mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
			}
			mBroadcasting = false;
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
		super.onInitializeAccessibilityEvent(event);
		event.setClassName(NoParentPressImageView.class.getName());
		event.setChecked(mChecked);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
		super.onInitializeAccessibilityNodeInfo(info);
		info.setClassName(NoParentPressImageView.class.getName());
		info.setCheckable(true);
		info.setChecked(mChecked);
	}

	/**
	 * Register a callback to be invoked when the checked state of this button
	 * changes.
	 *
	 * @param listener the callback to call on checked state change
	 */
	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mOnCheckedChangeListener = listener;
	}

	/**
	 * Interface definition for a callback to be invoked when the checked state
	 * of a compound button changed.
	 */
	public static interface OnCheckedChangeListener {
		/**
		 * Called when the checked state of a compound button has changed.
		 *
		 * @param buttonView The compound button view whose state has changed.
		 * @param isChecked  The new checked state of buttonView.
		 */
		void onCheckedChanged(NoParentPressImageView buttonView, boolean isChecked);
	}

	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	static class SavedState extends BaseSavedState {
		boolean checked;

		/**
		 * Constructor called from {@link net.bicou.redmine.widget.NoParentPressImageView#onSaveInstanceState()}
		 */
		SavedState(Parcelable superState) {
			super(superState);
		}

		/**
		 * Constructor called from {@link #CREATOR}
		 */
		private SavedState(Parcel in) {
			super(in);
			checked = (Boolean) in.readValue(null);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeValue(checked);
		}

		@Override
		public String toString() {
			return "NoParentPressImageView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " checked=" + checked + "}";
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.checked = isChecked();
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		setChecked(ss.checked);
		requestLayout();
	}
}
