package net.bicou.redmine.app.welcome;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

/**
* Created by bicou on 21/06/13.
*/
public class OverviewCard {
	int titleTextId;
	int subtitleTextId;
	int imageResId;
	int actionIconId;
	int actionTextId;
	Intent actionIntent;
	boolean useSecondAction;
	int action2IconId;
	int action2TextId;
	Intent action2Intent;

	View.OnClickListener mOnClickListener;

	public OverviewCard setTitle(final int title, final int subtitle, final int image) {
		titleTextId = title;
		subtitleTextId = subtitle;
		imageResId = image;
		return this;
	}

	public OverviewCard addAction(final int actionIcon, final int actionText, final Intent intent) {
		if (actionIconId == 0) {
			actionIconId = actionIcon;
			actionTextId = actionText;
			actionIntent = intent;
		} else {
			useSecondAction = true;
			action2IconId = actionIcon;
			action2TextId = actionText;
			action2Intent = intent;
		}
		return this;
	}

	public View getView(final Context context, final ViewGroup container) {
		TextView title, subTitle, action;
		ImageView image, actionIcon;

		final View card = LayoutInflater.from(context).inflate(R.layout.welcome_card, container, false);

		title = (TextView) card.findViewById(R.id.overview_card_title);
		title.setText(titleTextId);

		subTitle = (TextView) card.findViewById(R.id.overview_card_subtitle);
		subTitle.setId(subtitleTextId);

		image = (ImageView) card.findViewById(R.id.overview_card_image);
		image.setImageResource(imageResId);

		// First action icon
		mOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				try {
					context.startActivity(actionIntent);
				} catch (final Exception e) {
					L.e("Unable to start activity " + actionIntent, e);
				}
			}
		};
		action = (TextView) card.findViewById(R.id.overview_card_action);
		action.setText(actionTextId);
		action.setOnClickListener(mOnClickListener);
		card.setOnClickListener(mOnClickListener);
		card.findViewById(R.id.overview_card_image_holder).setOnClickListener(mOnClickListener);
		card.findViewById(R.id.overview_card_image).setOnClickListener(mOnClickListener);

		actionIcon = (ImageView) card.findViewById(R.id.overview_card_action_image);
		actionIcon.setImageResource(actionIconId);

		// Second action icon
		if (useSecondAction) {
			card.findViewById(R.id.overview_card_second_action_layout).setVisibility(View.VISIBLE);
			action = (TextView) card.findViewById(R.id.overview_card_second_action);
			action.setText(action2TextId);
			action.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					try {
						context.startActivity(action2Intent);
					} catch (final Exception e) {
						L.e("Unable to start activity " + action2Intent);
						// TODO: this message is relevant only for the add account, which is for now, the only second action on a card
						Toast.makeText(context, R.string.setup_sync_help, Toast.LENGTH_LONG).show();
					}
				}
			});

			actionIcon = (ImageView) card.findViewById(R.id.overview_card_second_action_image);
			actionIcon.setImageResource(action2IconId);
		}

		return card;
	}

	public View.OnClickListener getOnClickListener() {
		return mOnClickListener;
	}
}
