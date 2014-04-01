package net.bicou.redmine.data.json;

/**
 * Wrapper around the JSON string returned by the play store upon a valid in-app purchase
 * Created by bicou on 01/04/2014.
 */
public class InAppSkuDetails {
	public String productId;
	public String type;
	public String price;
	public String price_amount_micros;
	public String price_currency_code;
	public String title;
	public String description;
}
