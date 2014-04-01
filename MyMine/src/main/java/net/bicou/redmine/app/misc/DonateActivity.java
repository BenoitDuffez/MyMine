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
import com.google.gson.Gson;

import net.bicou.redmine.app.AsyncTaskFragment;
import net.bicou.redmine.data.json.InAppPurchaseData;
import net.bicou.redmine.data.json.InAppSkuDetails;
import net.bicou.redmine.util.L;

import java.util.ArrayList;

/**
 * Created by bicou on 17/06/13.
 */
public class DonateActivity extends ActionBarActivity implements AsyncTaskFragment.TaskFragmentCallbacks {
	public static final int ACTIVITY_REQUEST_CODE = 1001;
	private IInAppBillingService mService;
	private ServiceConnection mServiceConn;
	private static final int ACTION_CHECK_IAB = 0;
	private static final int ACTION_CHECK_PURCHASES = 1;
	private static final int ACTION_QUERY_ITEMS = 2;
	private static final int ACTION_BUY_ITEM = 3;

	private static final int IAB_API_VERSION = 3;

	private static final int BILLING_RESPONSE_RESULT_OK = 0;
	private static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
	private static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
	private static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
	private static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
	private static final int BILLING_RESPONSE_RESULT_ERROR = 6;
	private static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	private static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

	public static final String KEY_BUY_INTENT = "BUY_INTENT";
	public static final String KEY_RESPONSE_CODE = "RESPONSE_CODE";
	public static final String KEY_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
	public static final String KEY_INAPP_DATA_SIGNATURE = "INAPP_DATA_SIGNATURE";
	public static final String KEY_ITEM_ID_LIST = "ITEM_ID_LIST";
	public static final String KEY_DETAILS_LIST = "DETAILS_LIST";
	public static final String KEY_PRODUCT_ID = "productId";
	public static final String KEY_PRICE = "price";

	private static final String PURCHASE_TYPE_IN_APP = "inapp";

	public static final String DEVELOPER_PAYLOAD = "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ";
	private static final String SKU_DONATE_SMALL = "donate_small";

	private String mDonationPrice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, new DonateFragment()).commit();
		}

		mServiceConn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mService = IInAppBillingService.Stub.asInterface(service);
				AsyncTaskFragment.runTask(DonateActivity.this, 0, null);
			}
		};

		bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);

		AsyncTaskFragment.attachAsyncTaskFragment(this);
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
		L.d("starting action " + action);
	}

	@Override
	public Object doInBackGround(Context applicationContext, int action, Object parameters) {
		if (mService == null) {
			L.e("Can't get IAB to work with a null service!", null);
			return null;
		}

		switch (action) {
		case ACTION_CHECK_IAB:
			try {
				int billingSupported = mService.isBillingSupported(IAB_API_VERSION, getPackageName(), PURCHASE_TYPE_IN_APP);
				switch (billingSupported) {
				case BILLING_RESPONSE_RESULT_OK:
					return "0 - success ";
				case BILLING_RESPONSE_RESULT_USER_CANCELED:
					return "1 - user pressed back or canceled a dialog ";
				case BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE:
					return "3 - this billing API version is not supported for the type requested ";
				case BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE:
					return "4 - requested SKU is not available for purchase ";
				case BILLING_RESPONSE_RESULT_DEVELOPER_ERROR:
					return "5 - invalid arguments provided to the API ";
				case BILLING_RESPONSE_RESULT_ERROR:
					return "6 - Fatal error during the API action ";
				case BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
					return "7 - Failure to purchase since item is already owned";
				case BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED:
					return "8 - Failure to consume since item is not owned";
				default:
					return "Invalid isBillingSupported response code: " + billingSupported;
				}
			} catch (RemoteException e) {
				L.e("Unable to check whether billing is supported", e);
				return null;
			}

		case ACTION_CHECK_PURCHASES:
			try {
				return mService.getPurchases(IAB_API_VERSION, getPackageName(), PURCHASE_TYPE_IN_APP, null);
			} catch (RemoteException e) {
				L.e("Unable to retrieve the IAB purchases", e);
				return null;
			}

		case ACTION_QUERY_ITEMS:
			return doQueryItems();

		case ACTION_BUY_ITEM:
			purchaseItem(SKU_DONATE_SMALL);
			break;
		}

		return "no action";
	}

	@Override
	public void onPostExecute(int action, Object parameters, Object result) {
		L.d("action " + action + " result: " + result);
		if (result != null && result instanceof Bundle) {
			for (String key : ((Bundle) result).keySet()) {
				L.i(">" + key + ": " + ((Bundle) result).get(key));
			}
		}
		if (action < 4) {
			AsyncTaskFragment.runTask(this, action + 1, null);
		}
	}

	private Bundle doQueryItems() {
		ArrayList<String> skuList = new ArrayList<String>();
		skuList.add(SKU_DONATE_SMALL);
		Bundle querySkus = new Bundle();
		querySkus.putStringArrayList(KEY_ITEM_ID_LIST, skuList);

		Bundle skuDetails = null;
		try {
			skuDetails = mService.getSkuDetails(IAB_API_VERSION, getPackageName(), PURCHASE_TYPE_IN_APP, querySkus);

			int response = skuDetails.getInt(KEY_RESPONSE_CODE);
			if (response == 0) {
				ArrayList<String> responseList = skuDetails.getStringArrayList(KEY_DETAILS_LIST);
				if (responseList != null) {
					for (String thisResponse : responseList) {
						InAppSkuDetails details = new Gson().fromJson(thisResponse, InAppSkuDetails.class);
						L.d("response: " + details);
						mDonationPrice = details.price;
					}
				}
			}
		} catch (RemoteException e) {
			//TODO: handle error
			L.e("Unable to get SKU details", e);
		}

		return skuDetails;
	}

	private void purchaseItem(String sku) {
		try {
			Bundle buyIntentBundle = mService.getBuyIntent(IAB_API_VERSION, getPackageName(), sku, PURCHASE_TYPE_IN_APP, DEVELOPER_PAYLOAD);
			PendingIntent pendingIntent = buyIntentBundle.getParcelable(KEY_BUY_INTENT);
			int responseCode = buyIntentBundle.getInt(KEY_RESPONSE_CODE);

			if (pendingIntent != null) {
				startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
			} else {
				L.e("Unable to retrieve buy intent", null);
				L.i("bundle: " + buyIntentBundle);
				for (String key : buyIntentBundle.keySet()) {
					L.i(">" + key + ": " + buyIntentBundle.get(key));
				}
			}
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
		if (requestCode == ACTIVITY_REQUEST_CODE) {
			L.d("data=" + data);
			L.i("extras:");
			if (data.getExtras() != null) {
				for (String key : data.getExtras().keySet()) {
					L.i("#" + key + ": " + data.getExtras().get(key));
				}
			}

			int responseCode = data.getIntExtra(KEY_RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK);
			String purchaseData = data.getStringExtra(KEY_INAPP_PURCHASE_DATA);
			String dataSignature = data.getStringExtra(KEY_INAPP_DATA_SIGNATURE);

			if (resultCode == BILLING_RESPONSE_RESULT_OK) {
				InAppPurchaseData inAppPurchaseData = new Gson().fromJson(purchaseData, InAppPurchaseData.class);
				L.d("Bought " + inAppPurchaseData);
			} else {
				L.d("Didn't buy, code=" + responseCode + ", purchaseData=" + purchaseData + ", dataSignature=" + dataSignature);
			}
		}
	}
}
