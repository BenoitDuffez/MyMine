package net.bicou.redmine.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RobotoLightTextView extends CustomTypefaceTextView {
	public RobotoLightTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void createFont() {
		super.createFont("Roboto-Light.ttf");
	}
}
