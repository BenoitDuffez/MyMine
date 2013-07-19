package net.bicou.redmine.app.welcome;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.origamilabs.library.views.StaggeredGridView;
import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

import java.util.List;

/**
 * Created by bicou on 21/06/13.
 */
public class CardsAdapter extends BaseAdapter {
	private List<OverviewCard> mCards;
	CardActionCallback mCallback;
	private static final int TAG_VIEW_HOLDER = 0;
	private static final int TAG_CARD = 1;
	private static final int TAG_MENU = 2;
	private static final int TAG_ACTION = 3;

	public interface CardActionCallback {
		public void onActionSelected(int actionId);
	}

	public CardsAdapter(CardActionCallback callback) {
		mCallback = callback;
	}

	public void setData(List<OverviewCard> cards) {
		mCards = cards;
		notifyDataSetChanged();
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
		View layout;
		TextView title, description;
		ImageView image, icon, overflowIcon;
		ViewGroup overflowMenu;
	}

	private View.OnClickListener mActionItemOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			Integer id = (Integer) view.getTag(TAG_ACTION);
			mCallback.onActionSelected(id);
		}
	};

	private View.OnClickListener mOverflowMenuOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			View menu = (View) view.getTag(TAG_MENU);
			menu.setVisibility(menu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
		}
	};

	private View.OnClickListener mItemClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			OverviewCard card = (OverviewCard) view.getTag(TAG_CARD);
			view.getContext().startActivity(card.getDefaultAction());
		}
	};

	private StaggeredGridView.OnItemClickListener mStaggeredItemClickListener = new StaggeredGridView.OnItemClickListener() {
		@Override
		public void onItemClick(StaggeredGridView parent, View view, int position, long id) {
			OverviewCard card = getItem(position);
			if (card != null && card.isEnabled()) {
				Context context = parent == null ? null : parent.getContext();
				if (context != null) {
					Intent intent = card.getDefaultAction();
					if (intent != null) {
						context.startActivity(intent);
						return;
					}
				}
			}
			L.i("didn't trigger any action on this SGV item");
		}
	};

	public StaggeredGridView.OnItemClickListener getStaggeredItemClickListener() {
		return mStaggeredItemClickListener;
	}

	public void bindView(final LayoutInflater layoutInflater, final CardViewsHolder holder, final OverviewCard card) {
		holder.title.setText(card.titleTextId);
		holder.description.setText(card.description);
		if (card.imageResId > 0) {
			holder.image.setImageResource(card.imageResId);
			holder.image.setVisibility(View.VISIBLE);
		} else {
			holder.image.setVisibility(View.GONE);
		}
		holder.icon.setImageResource(card.iconId);

		// Actions?
		boolean hasActions = card.actions != null && card.actions.size() > 0;
		holder.overflowIcon.setVisibility(hasActions ? View.VISIBLE : View.INVISIBLE);
		holder.overflowMenu.setVisibility(hasActions ? View.INVISIBLE : View.GONE);

		holder.overflowIcon.setTag(TAG_MENU, holder.overflowMenu);
		holder.overflowIcon.setOnClickListener(mOverflowMenuOnClickListener);

		holder.layout.setTag(TAG_CARD, card);
		holder.layout.setOnClickListener(mItemClickListener);

		if (hasActions) {
			View actionView;
			TextView tv;
			for (final OverviewCard.CardAction action : card.actions) {
				actionView = layoutInflater.inflate(R.layout.overview_card_overflow_action, holder.overflowMenu, false);
				tv = (TextView) actionView.findViewById(R.id.overview_card_overflow_action);
				tv.setText(action.mTextResId);
				actionView.setTag(TAG_ACTION, action.mId);
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
			holder.layout = card;
			holder.title = (TextView) card.findViewById(R.id.overview_card_title);
			holder.description = (TextView) card.findViewById(R.id.overview_card_description);
			holder.image = (ImageView) card.findViewById(R.id.overview_card_image);
			holder.icon = (ImageView) card.findViewById(R.id.overview_card_icon);
			holder.overflowIcon = (ImageView) card.findViewById(R.id.overview_card_overflow_icon);
			holder.overflowMenu = (ViewGroup) card.findViewById(R.id.overview_card_overflow_menu);
			card.setTag(TAG_VIEW_HOLDER, holder);
		} else {
			card = convertView;
			holder = (CardViewsHolder) card.getTag(TAG_VIEW_HOLDER);
		}

		OverviewCard item = getItem(i);
		if (item != null) {
			bindView(layoutInflater, holder, item);
		}

		return card;
	}
}
