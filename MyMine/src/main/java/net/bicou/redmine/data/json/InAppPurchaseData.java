package net.bicou.redmine.data.json;

/**
 * Wrapper around the JSON string returned by the play store upon a valid in-app purchase
 * Created by bicou on 01/04/2014.
 */
public class InAppPurchaseData {
	public static final int PURCHASE_STATE_PURCHASED = 0;
	public static final int PURCHASE_STATE_CANCELLED = 1;
	public static final int PURCHASE_STATE_REFUNDED = 2;

	public String orderId;
	public String packageName;
	public String productId;
	public long purchaseTime;

	/**
	 * See {@link #PURCHASE_STATE_PURCHASED}, {@link #PURCHASE_STATE_CANCELLED} and {@link #PURCHASE_STATE_REFUNDED}
	 */
	public int purchaseState;
	public String developerPayload;
	public String purchaseToken;
}
