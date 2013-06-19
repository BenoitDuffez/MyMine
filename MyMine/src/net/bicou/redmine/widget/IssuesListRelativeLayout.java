package net.bicou.redmine.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import net.bicou.redmine.R;

/**
 * Created by bicou on 19/06/13.
 */
public class IssuesListRelativeLayout extends RelativeLayout {
	private static final int[] STATE_ISSUE_CLOSED = { R.attr.state_closed };
	boolean mIsClosed;

	public IssuesListRelativeLayout(Context context) {
		super(context);
	}

	public IssuesListRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IssuesListRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		if (mIsClosed) {
			// We are going to add 1 extra state.
			final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

			mergeDrawableStates(drawableState, STATE_ISSUE_CLOSED);

			return drawableState;
		} else {
			return super.onCreateDrawableState(extraSpace);
		}
	}

	public void setIsClosed(boolean isClosed) {
		mIsClosed = isClosed;
		refreshDrawableState();
	}
}
