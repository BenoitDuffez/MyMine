package net.bicou.redmine.app.ssl;

import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.android.splitscreen.SplitActivity;
import net.bicou.redmine.R;
import net.bicou.redmine.util.L;

import java.util.List;

public class CertificatesListFragment extends SherlockListFragment implements LoaderCallbacks<List<CertInfo>> {
	KeyStoreAdapter mAdapter;
	ListView mListView;
	ActionMode mActionMode;

	public static CertificatesListFragment newInstance(final Bundle args) {
		final CertificatesListFragment f = new CertificatesListFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		L.d("");
		final View v = inflater.inflate(R.layout.frag_keystore_cert_list, container, false);
		mListView = (ListView) v.findViewById(android.R.id.list);
		return v;
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAdapter = new KeyStoreAdapter(getActivity());
		mListView.setAdapter(mAdapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Bundle args = new Bundle();
		CertInfo item = mAdapter.getItem(position);
		args.putString(CertificateFragment.KEY_CERT_ALIAS, item == null ? null : item.getAlias());
		((SplitActivity) getActivity()).selectContent(args);
	}

	@Override
	public Loader<List<CertInfo>> onCreateLoader(final int id, final Bundle args) {
		return new KeyStoreLoader(getActivity());
	}

	@Override
	public void onLoadFinished(final Loader<List<CertInfo>> loader, final List<CertInfo> data) {
		mAdapter.setData(data);
	}

	@Override
	public void onLoaderReset(final Loader<List<CertInfo>> loader) {
		mAdapter.setData(null);
	}

	public void refreshList() {
		getLoaderManager().restartLoader(0, null, this).forceLoad();
	}

	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
			// Inflate a menu resource providing context menu items
			final MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.context_menu_list_certs, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_certs_delete:
				// shareCurrentItem();

				mode.finish(); // Action picked, so close the CAB
				return true;
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(final ActionMode mode) {
			mActionMode = null;
		}
	};
}
