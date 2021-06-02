package com.pixite.android.billingx

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams

abstract class BillingStore {

  companion object {
    private val lock = Any()
    internal var INSTANCE: BillingStore? = null

    fun defaultStore(context: Context): BillingStore {
      if (INSTANCE == null) {
        synchronized(lock) {
          if (INSTANCE == null) {
            INSTANCE = BillingStoreImpl(context
                .getSharedPreferences("dbx", AppCompatActivity.MODE_PRIVATE))
          }
        }
      }
      return INSTANCE!!
    }
  }

  abstract fun getSkuDetails(params: SkuDetailsParams): List<SkuDetails>
  abstract fun addProduct(skuDetails: SkuDetails): BillingStore
  abstract fun removeProduct(sku: String): BillingStore
  abstract fun clearProducts(): BillingStore

  abstract fun addPurchase(purchase: Purchase): BillingStore
  abstract fun clearPurchases(): BillingStore
  abstract fun getPurchases(@BillingClient.SkuType skuType: String): Purchase.PurchasesResult
  abstract fun getPurchaseHistoryRecords(@BillingClient.SkuType skuType: String): List<PurchaseHistoryRecord>
  abstract fun getPurchaseByToken(purchaseToken: String): Purchase?
  abstract fun removePurchase(purchaseToken: String): BillingStore

  abstract fun acknowledgePurchase(purchaseToken: String)
}