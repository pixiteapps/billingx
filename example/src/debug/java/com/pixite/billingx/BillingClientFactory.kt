package com.pixite.billingx

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.pixite.android.billingx.BillingStore
import com.pixite.android.billingx.DebugBillingClient
import com.pixite.android.billingx.SkuDetailsBuilder

class BillingClientFactory {

  fun createBillingClient(activity: Activity, updateListener: PurchasesUpdatedListener): BillingClient {
    initializeData(activity)
    return DebugBillingClient(activity, updateListener)
  }

  private fun initializeData(activity: Activity) {
    BillingStore.getInstance(activity)
        .clearProducts()
        .addProduct(
            SkuDetailsBuilder(
                sku = BillingManager.SKU_SUBS,
                type = BillingClient.SkuType.SUBS,
                price = "$9.99",
                priceAmountMicros = 9990000,
                priceCurrencyCode = "USD",
                title = "Premium Access",
                description = "Get all the super cool features.",
                subscriptionPeriod = "p1m",
                freeTrialPeriod = "p1w"
            ).build()
        )
  }
}