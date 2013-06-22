package net.bicou.redmine.app.welcome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

import java.util.List;

/**
 * Created by bicou on 21/06/13.
 */
public class CardsAdapter extends BaseAdapter {
	private List<OverviewCard> mCards;
	CardActionCallback mCallback;

	public interface CardActionCallback {
		public void onActionSelected(int actionId);
	}

	public CardsAdapter(List<OverviewCard> cards, CardActionCallback callback) {
		mCards = cards;
		mCallback = callback;
	}

	@Override
	public int getCount() {
		return mCards == null ? 0 : mCards.size();
	}

	@Override
	public OverviewCard getItem(int i) {
		return i >= getCount() || i < 0 ? null : mCards.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	private static class CardViewsHolder {
		TextView title, description;
		ImageView image, icon, overflowIcon;
		ViewGroup overflowMenu;
	}

	private View.OnClickListener mActionItemOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			L.d("item click on: " + view + " tag=" + view.getTag());
			Integer id = (Integer) view.getTag();
			mCallback.onActionSelected(id);
		}
	};

	private View.OnClickListener mOverflowMenuOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			L.d("menu click on: " + view + " tag=" + view.getTag());
			View menu = (View) view.getTag();
			menu.setVisibility(menu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
		}
	};

	public void bindView(final LayoutInflater layoutInflater, final CardViewsHolder holder, final OverviewCard card) {
		holder.title.setText(card.titleTextId);
		holder.description.setText(card.description);
		holder.image.setImageResource(card.imageResId);
		holder.icon.setImageResource(card.iconId);

		// Actions?
		boolean hasActions = card.actions != null && card.actions.size() > 0;
		holder.overflowIcon.setVisibility(hasActions ? View.VISIBLE : View.INVISIBLE);
		holder.overflowMenu.setVisibility(hasActions ? View.INVISIBLE : View.GONE);

		holder.overflowIcon.setTag(holder.overflowMenu);
		holder.overflowIcon.setOnClickListener(mOverflowMenuOnClickListener);

		if (hasActions) {
			View actionView;
			TextView tv;
			for (final OverviewCard.CardAction action : card.actions) {
				actionView = layoutInflater.inflate(R.layout.overview_card_overflow_action, holder.overflowMenu, false);
				tv = (TextView) actionView.findViewById(R.id.overview_card_overflow_action);
				tv.setText(action.mTextResId);
				actionView.setTag(action.mId);
				actionView.setOnClickListener(mActionItemOnClickListener);
				holder.overflowMenu.addView(actionView);
			}
		}
	}

	@Override
	public View getView(int i, View convertView, ViewGroup viewGroup) {
		View card;
		CardViewsHolder holder;
		LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());

		if (convertView == null) {
			card = layoutInflater.inflate(R.layout.welcome_card, viewGroup, false);
			holder = new CardViewsHolder();
			holder.title = (TextView) card.findViewById(R.id.overview_card_title);
			holder.description = (TextView) card.findViewById(R.id.overview_card_description);
			holder.image = (ImageView) card.findViewById(R.id.overview_card_image);
			holder.icon = (ImageView) card.findViewById(R.id.overview_card_icon);
			holder.overflowIcon = (ImageView) card.findViewById(R.id.overview_card_overflow_icon);
			holder.overflowMenu = (ViewGroup) card.findViewById(R.id.overview_card_overflow_menu);
			card.setTag(holder);
		} else {
			card = convertView;
			holder = (CardViewsHolder) card.getTag();
		}

		OverviewCard item = getItem(i);
		if (item != null) {
			bindView(layoutInflater, holder, item);
		}

		return card;
	}
}
