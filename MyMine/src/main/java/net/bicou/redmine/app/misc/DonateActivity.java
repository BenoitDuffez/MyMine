package net.bicou.redmine.app.misc;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;

import com.android.vending.billing.IInAppBillingService;

import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.util.L;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bicou on 17/06/13.
 */
public class DonateActivity extends ActionBarActivity implements AsyncTaskFragment.TaskFragmentCallbacks {
	private IInAppBillingService mService;
	private ServiceConnection mServiceConn;
	private static final int ACTION_QUERY_ITEMS = 0;
	private static final String SKU_DONATE_SMALL = "donate_small";
	private String mDonationPrice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, new DonateFragment()).commit();
		}

		AsyncTaskFragment.attachAsyncTaskFragment(this);
		mServiceConn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mService = IInAppBillingService.Stub.asInterface(service);
			}
		};

		bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);
	}

	private void queryItems() {
		AsyncTaskFragment.runTask(this, ACTION_QUERY_ITEMS, null);
	}

	private void purchaseItem(String sku) {
		try {
			Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");

			PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

			startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
		} catch (IntentSender.SendIntentException e) {
			// TODO: handle
			L.e("Unable to start buying activity", e);
		} catch (RemoteException e) {
			//TODO
			L.e("Unable to get buy intent", e);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1001) {
			int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
			String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
			String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

			if (resultCode == RESULT_OK) {
				try {
					JSONObject jo = new JSONObject(purchaseData);
					String sku = jo.getString("productId");
					//					alert("You have bought the " + sku + ". Excellent choice,  adventurer !");
				} catch (JSONException e) {
					//					alert("Failed to parse purchase data.");
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			unbindService(mServiceConn);
		}
	}

	@Override
	public void onPreExecute(int action, Object parameters) {
	}

	@Override
	public Object doInBackGround(Context applicationContext, int action, Object parameters) {
		ArrayList<String> skuList = new ArrayList<String>();
		skuList.add(SKU_DONATE_SMALL);
		Bundle querySkus = new Bundle();
		querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

		Bundle skuDetails = null;
		try {
			skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);

			int response = skuDetails.getInt("RESPONSE_CODE");
			if (response == 0) {
				ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
				if (responseList != null) {

					for (String thisResponse : responseList) {
						JSONObject object = new JSONObject(thisResponse);
						String sku = object.getString("productId");
						String price = object.getString("price");
						mDonationPrice = price;
					}
				}
			}
		} catch (RemoteException e) {
			//TODO: handle error
			L.e("Unable to get SKU details", e);
		} catch (JSONException e) {
			//TODO: handle error
			L.e("Unable to parse server response", e);
		}

		return skuDetails;
	}

	@Override
	public void onPostExecute(int action, Object parameters, Object result) {
		Bundle skuDetails = (Bundle) result;
		if (skuDetails == null) {
			//TODO
			return;
		}

	}
}
