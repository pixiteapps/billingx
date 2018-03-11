package com.ryanharter.billingx

import android.content.Context
import android.support.v7.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams

abstract class BillingStore {

  companion object {
    private val lock = Any()
    internal var INSTANCE: BillingStore? = null

    fun getInstance(context: Context): BillingStore {
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
  abstract fun getPurchases(@BillingClient.SkuType skuType: String): Purchase.PurchasesResult
  abstract fun addProduct(skuDetails: SkuDetails): BillingStore
  abstract fun removeProduct(sku: String): BillingStore
  abstract fun clearProducts(): BillingStore
  abstract fun addPurchase(purchase: Purchase): BillingStore
  abstract fun removePurchase(sku: String): BillingStore
  abstract fun clearPurchases(): BillingStore
}