package com.pixite.android.billingx

import com.android.billingclient.api.Purchase
import org.json.JSONObject

data class PurchaseBuilder(
        val orderId: String? = null,
        val packageName: String? = null,
        val skus: List<String>,
        val purchaseTime: Long,
        val purchaseToken: String,
        val signature: String,
        val purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        val acknowledged: Boolean = false,
        val isAutoRenewing: Boolean? = null,
        val developerPayload: Map<String, Any>? = null
) {

  fun build(): Purchase {
    val json = JSONObject()
    orderId?.let { json.put("orderId", it) }
    packageName?.let { json.put("packageName", it) }
    if (skus.size > 1) {
      json.put("productIds", skus)
    } else {
      json.put("productId", skus.first())
    }
    json.put("purchaseTime", purchaseTime)
    json.put("purchaseToken", purchaseToken)
    json.put("acknowledged", acknowledged)
    json.put("purchaseState", purchaseState)
    isAutoRenewing?.let { json.put("autoRenewing", it) }
    developerPayload?.let { json.put("developerPayload", it) }
    return Purchase(json.toString(), signature)
  }

  companion object {
    fun from(purchase: Purchase): PurchaseBuilder {
      return PurchaseBuilder(
              orderId = purchase.orderId,
              packageName = purchase.packageName,
              skus = purchase.skus,
              purchaseTime = purchase.purchaseTime,
              purchaseToken = purchase.purchaseToken,
              signature = purchase.signature,
              purchaseState = purchase.purchaseState,
              acknowledged = purchase.isAcknowledged,
              isAutoRenewing = purchase.isAutoRenewing
      )
    }
  }
}