package net.bicou.redmine.app.ssl;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.bicou.redmine.R;
import net.bicou.redmine.app.ssl.CertificatesListFragment.CertInfo;
import net.bicou.redmine.net.ssl.KeyStoreDiskStorage;
import net.bicou.redmine.net.ssl.MyMineSSLKeyManager;
import net.bicou.redmine.util.L;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
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
		mListView.setLongClickable(true);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> av, final View view, final int position, final long id) {
				if (mActionMode != null) {
					view.setSelected(true);
				} else {
					// Build fragment
					final Bundle args = new Bundle();
					args.putString(CertificateFragment.KEY_CERT_ALIAS, mAdapter.getAlias(position));
					final CertificateFragment frag = CertificateFragment.newInstance(args);

					// Open fragment in the right pane?
					//TODO
					//if (((AbsMyMineActivity) getActivity()).isSplitScreen()) {
					//	getFragmentManager().beginTransaction().replace(R.id.certificates_pane_certificate, frag).commit();
					//} else {
					getFragmentManager().beginTransaction().replace(R.id.certificates_pane_list, frag).addToBackStack("prout").commit();
					//}
				}
			}
		});
		// if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
		// mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		// @Override
		// @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		// public boolean onItemLongClick(final AdapterView<?> av, final View view, final int position, final long id) {
		// L.d("");
		// if (mActionMode != null) {
		// return false;
		// }
		//
		// mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
		// view.setActivated(!view.isActivated());
		// return true;
		// }
		// });
		// }

		getLoaderManager().initLoader(0, null, this);
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
		getLoaderManager().restartLoader(0, null, this);
	}

	public static class CertInfo {
		KeyStoreLoader mLoader;
		X509Certificate mCertificate;
		String mAlias;

		public CertInfo(final KeyStoreLoader loader, final X509Certificate cert, final String alias) {
			L.d("");
			mLoader = loader;
			mCertificate = cert;
			mAlias = alias;
		}

		public String getLabel() {
			if (mCertificate != null) {
				return mCertificate.getIssuerX500Principal().toString();
			}
			return "";
		}

		public String getAlias() {
			return mAlias;
		}
	}

	public static class KeyStoreLoader extends AsyncTaskLoader<List<CertInfo>> {
		Context mContext;
		List<CertInfo> mData;

		public KeyStoreLoader(final Context context) {
			super(context);
			L.d("");
			mContext = context;
		}

		@Override
		public List<CertInfo> loadInBackground() {
			mData = new ArrayList<CertInfo>();

			loadFromAppKeyStore();

			if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
				loadFromSystem();
			}

			return mData;
		}

		private void loadFromAppKeyStore() {
			final KeyStoreDiskStorage ds = new KeyStoreDiskStorage(mContext);
			final KeyStore keyStore = ds.loadAppKeyStore();
			try {
				final Enumeration<String> aliases = keyStore.aliases();
				while (aliases.hasMoreElements()) {
					final String alias = aliases.nextElement();
					final X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
					mData.add(new CertInfo(this, cert, alias));
				}
			} catch (final KeyStoreException e) {
				// TODO
			} catch (final ClassCastException e) {
				// TODO
			}
		}

		@TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
		private void loadFromSystem() {
			try {
				final MyMineSSLKeyManager km = MyMineSSLKeyManager.fromAlias(mContext);
				if (km == null) {
					return;
				}

				final X509Certificate[] certs = km.getCertificateChain(null);
				if (certs == null) {
					return;
				}

				final String alias = km.chooseClientAlias(null, null, null);
				if (alias == null) {
					return;
				}

				for (final X509Certificate cert : certs) {
					mData.add(new CertInfo(this, cert, alias));
				}
			} catch (final CertificateException e) {
				// TODO
			}
		}

		@Override
		protected void onStartLoading() {
			if (mData != null) {
				deliverResult(mData);
			} else {
				forceLoad();
			}
		}
	}

	public static class KeyStoreAdapter extends ArrayAdapter<CertInfo> {
		LayoutInflater mInflater;

		public KeyStoreAdapter(final Context context) {
			super(context, R.layout.listview_item);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View view;

			if (convertView == null) {
				view = mInflater.inflate(R.layout.listview_item, parent, false);
			} else {
				view = convertView;
			}

			final CertInfo item = getItem(position);
			((TextView) view.findViewById(android.R.id.text1)).setText(item.getLabel());

			return view;
		}

		public String getAlias(final int position) {
			final CertInfo item = getItem(position);
			if (item != null) {
				return item.getAlias();
			}
			return null;
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		public void setData(final List<CertInfo> data) {
			clear();
			if (data != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					addAll(data);
				} else {
					for (final CertInfo item : data) {
						add(item);
					}
				}
			}
		}
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
