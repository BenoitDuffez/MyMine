package net.bicou.redmine.app.misc;

import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LoadingFragment extends Fragment {
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
