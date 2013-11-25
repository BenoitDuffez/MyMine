package net.bicou.redmine.app.welcome;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bicou on 21/06/13.
 */
public class OverviewCard {
	int titleTextId;
	String description;
	int imageResId;
	int iconId;
	List<CardAction> actions;
	Intent defaultAction;

	boolean enabled;

	public static class CardAction {
		Object mId;
		int mTextResId;

		public CardAction(final Object id, final int text) {
			mId = id;
			mTextResId = text;
		}
	}

	public OverviewCard(Intent i) {
		actions = new ArrayList<CardAction>();
		defaultAction = i;
	}

	public OverviewCard setContent(final int title, final String des, final int image, final int icon) {
		titleTextId = title;
		description = des;
		imageResId = image;
		iconId = icon;
		return this;
	}

	public OverviewCard addAction(final Object id, final int actionText) {
		actions.add(new CardAction(id, actionText));
		return this;
	}

	public Intent getDefaultAction() {
		return defaultAction;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}
}
