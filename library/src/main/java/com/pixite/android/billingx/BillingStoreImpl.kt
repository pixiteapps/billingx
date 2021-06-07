package com.pixite.android.billingx

import android.content.SharedPreferences
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.InternalPurchasesResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchasesResult
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import org.json.JSONArray
import org.json.JSONObject

class BillingStoreImpl(private val prefs: SharedPreferences) : BillingStore(){

  companion object {
    internal const val KEY_PURCHASES = "dbc_purchases"
    internal const val KEY_SKU_DETAILS = "dbc_sku_details"
    internal const val KEY_CONNECTION_STATE = "dbc_connection_state"
  }

  override fun getSkuDetails(params: SkuDetailsParams): List<SkuDetails> {
    return prefs.getString(KEY_SKU_DETAILS, "[]")?.toSkuDetailsList()
        ?.filter { it.sku in params.skusList && it.type == params.skuType }
        .orEmpty()
  }

  override fun getPurchases(@SkuType skuType: String): PurchasesResult {
    return InternalPurchasesResult(BillingClient.BillingResponseCode.OK,
        prefs.getString(KEY_PURCHASES, "[]")?.toPurchaseList()
            ?.filter { it.signature.endsWith(skuType) })
  }

  override fun getPurchaseHistoryRecords(skuType: String): List<PurchaseHistoryRecord> {
    return prefs.getString(KEY_PURCHASES, "[]")?.toPurchaseHistoryRecordList()
            ?.filter { it.signature.endsWith(skuType) }
            .orEmpty()
  }

  override fun getPurchaseByToken(purchaseToken: String): Purchase? {
    return prefs.getString(KEY_PURCHASES, "[]")?.toPurchaseList()
        ?.firstOrNull { it.purchaseToken == purchaseToken }
  }

  override fun addProduct(skuDetails: SkuDetails): BillingStore {
    val allDetails = JSONArray(prefs.getString(KEY_SKU_DETAILS, "[]"))
    allDetails.put(skuDetails.toJSONObject())
    prefs.edit().putString(KEY_SKU_DETAILS, allDetails.toString()) .apply()
    return this
  }

  override fun removeProduct(sku: String): BillingStore {
    val allDetails = prefs.getString(KEY_SKU_DETAILS, "[]")?.toSkuDetailsList()
    val filtered = allDetails?.filter { it.sku != sku }
    val json = JSONArray()
    filtered?.forEach { json.put(it.toJSONObject()) }
    prefs.edit().putString(KEY_SKU_DETAILS, json.toString()).apply()
    return this
  }

  override fun clearProducts(): BillingStore {
    prefs.edit().remove(KEY_SKU_DETAILS).apply()
    return this
  }

  override fun addPurchase(purchase: Purchase): BillingStore {
    val allPurchases = JSONArray(prefs.getString(KEY_PURCHASES, "[]"))
    allPurchases.put(purchase.toJSONObject())
    prefs.edit().putString(KEY_PURCHASES, allPurchases.toString()) .apply()
    return this
  }

  override fun removePurchase(purchaseToken: String): BillingStore {
    val allPurchases = prefs.getString(KEY_PURCHASES, "[]")?.toPurchaseList()
    val filtered = allPurchases?.filter { it.purchaseToken != purchaseToken }
    val json = JSONArray()
    filtered?.forEach { json.put(it.toJSONObject()) }
    prefs.edit().putString(KEY_PURCHASES, json.toString()).apply()
    return this
  }

  override fun clearPurchases(): BillingStore {
    prefs.edit().remove(KEY_PURCHASES).apply()
    return this
  }

  override fun acknowledgePurchase(purchaseToken: String) {
    val allPurchases = prefs.getString(KEY_PURCHASES, "[]")?.toPurchaseList()
    val acknowledgedPurchases = allPurchases?.map {
      if (it.purchaseToken != purchaseToken) return@map it
      PurchaseBuilder.from(it).copy(acknowledged = true).build()
    }
    val json = JSONArray()
    acknowledgedPurchases?.forEach { json.put(it.toJSONObject()) }
    prefs.edit().putString(KEY_PURCHASES, json.toString()).apply()
  }

  override fun setConnectionState(connectionState: Int): BillingStore {
    prefs.edit().putInt(KEY_CONNECTION_STATE, connectionState).apply()
    return this
  }

  override fun getConnectionState(): Int {
      return prefs.getInt(KEY_CONNECTION_STATE, BillingClient.ConnectionState.CONNECTED)
  }

  private fun Purchase.toJSONObject(): JSONObject =
      JSONObject().put("purchase", JSONObject(originalJson)).put("signature", signature)

  private fun JSONObject.toPurchase(): Purchase =
      Purchase(this.getJSONObject("purchase").toString(), this.getString("signature"))

  private fun JSONObject.toPurchaseHistoryRecord() :PurchaseHistoryRecord =
          PurchaseHistoryRecord(this.getJSONObject("purchase").toString(), this.getString("signature"))

  private fun SkuDetails.toJSONObject(): JSONObject = JSONObject(originalJson)

  private fun JSONObject.toSkuDetails(): SkuDetails = SkuDetails(toString())

  private fun String.toPurchaseList(): List<Purchase> {
    val list = mutableListOf<Purchase>()
    val array = JSONArray(this)
    (0 until array.length()).mapTo(list) { array.getJSONObject(it).toPurchase() }
    return list
  }

  private fun String.toPurchaseHistoryRecordList(): List<PurchaseHistoryRecord> {
    val list = mutableListOf<PurchaseHistoryRecord>()
    val array = JSONArray(this)
    (0 until array.length()).mapTo(list) { array.getJSONObject(it).toPurchaseHistoryRecord() }
    return list
  }

  private fun String.toSkuDetailsList(): List<SkuDetails> {
    val list = mutableListOf<SkuDetails>()
    val array = JSONArray(this)
    (0 until array.length()).mapTo(list) { array.getJSONObject(it).toSkuDetails() }
    return list
  }

}