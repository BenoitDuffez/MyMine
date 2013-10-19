package net.bicou.redmine.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RobotoBoldTextView extends CustomTypefaceTextView {
	public RobotoBoldTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void createFont() {
		super.createFont("Roboto-Bold.ttf");
	}
}
