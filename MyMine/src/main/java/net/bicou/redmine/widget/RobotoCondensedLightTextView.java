package net.bicou.redmine.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RobotoCondensedLightTextView extends CustomTypefaceTextView {
	public RobotoCondensedLightTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void createFont() {
		super.createFont("RobotoCondensed-Light.ttf");
	}
}
