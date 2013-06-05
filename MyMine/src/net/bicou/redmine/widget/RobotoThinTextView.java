package net.bicou.redmine.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RobotoThinTextView extends CustomTypefaceTextView {
	public RobotoThinTextView(final Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void createFont() {
		super.createFont("Roboto-Thin.ttf");
	}
}
