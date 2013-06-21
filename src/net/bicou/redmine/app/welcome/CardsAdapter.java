package net.bicou.redmine.app.welcome;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
* Created by bicou on 21/06/13.
*/
public class CardsAdapter extends BaseAdapter {
	private List<OverviewCard> mCards;

	public CardsAdapter(List<OverviewCard> cards) {
		mCards = cards;
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

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		OverviewCard item = getItem(i);
		if (item != null) {
			return item.getView(viewGroup.getContext(), viewGroup);
		}
		return null;
	}
}
