package net.bicou.redmine.app.misc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ga.TrackedFragment;
import net.bicou.redmine.util.L;

public class LoadingFragment extends TrackedFragment {
	public static LoadingFragment newInstance() {
		final LoadingFragment f = new LoadingFragment();
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_loading, container, false);
		L.d("");
		return v;
	}
}
