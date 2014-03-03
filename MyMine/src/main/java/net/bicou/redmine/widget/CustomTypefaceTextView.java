package net.bicou.redmine.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public abstract class CustomTypefaceTextView extends TextView {
	private final Context mContext;

	public CustomTypefaceTextView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		createFont();
	}

	public CustomTypefaceTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		createFont();
	}

	public CustomTypefaceTextView(final Context context) {
		super(context);
		mContext = context;
		createFont();
	}

	/**
	 * This one must call super.createFont(String)
	 */
	protected abstract void createFont();

	public void createFont(String typeface) {
		if (isInEditMode()) {
			return;
		}
		try {
			final Typeface font = Typeface.createFromAsset(mContext.getAssets(), typeface);
			Typeface currentFont = getTypeface();
			if (currentFont != null) {
				setTypeface(font, currentFont.getStyle());
			} else {
				setTypeface(font);
			}
		} catch (final Exception e) {
		}
	}
}
