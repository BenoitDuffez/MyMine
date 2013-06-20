package net.bicou.redmine.app.misc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockFragment;
import net.bicou.redmine.R;

/**
 * Created by bicou on 20/06/13.
 */
public class EmptyFragment extends SherlockFragment {
	int mResId;

	public EmptyFragment(int resId) {
		mResId = resId;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_empty, container, false);
		ImageView iv = (ImageView) v.findViewById(R.id.frag_empty_image);
		iv.setImageResource(mResId);
		return v;
	}
}
