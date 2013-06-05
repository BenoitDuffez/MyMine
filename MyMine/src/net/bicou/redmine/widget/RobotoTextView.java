package net.bicou.redmine.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RobotoTextView extends CustomTypefaceTextView {
	public RobotoTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void createFont() {
		super.createFont("Roboto-Regular.ttf");
	}
}
