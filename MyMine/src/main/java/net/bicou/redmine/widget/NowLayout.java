package net.bicou.redmine.widget;

import net.bicou.redmine.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

public class NowLayout extends LinearLayout implements OnGlobalLayoutListener {

	public NowLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		initLayoutObserver();
	}

	public NowLayout(final Context context) {
		super(context);
		initLayoutObserver();
	}

	private void initLayoutObserver() {
		// force vertical orientation and add observer
		// setOrientation(LinearLayout.VERTICAL);
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@Override
	public void onGlobalLayout() {
		getViewTreeObserver().removeGlobalOnLayoutListener(this);

		final int heightPx = getContext().getResources().getDisplayMetrics().heightPixels;

		boolean inversed = false;
		final int childCount = getChildCount();

		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);

			final int[] location = new int[2];

			child.getLocationOnScreen(location);

			if (location[1] > heightPx) {
				break;
			}

			if (!inversed) {
				child.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_left));
			} else {
				child.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_right));
			}

			inversed = !inversed;
		}
	}
}
