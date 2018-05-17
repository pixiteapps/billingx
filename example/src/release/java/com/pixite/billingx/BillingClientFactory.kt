package com.pixite.billingx

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

class BillingClientFactory {
  fun createBillingClient(activity: Activity, updateListener: PurchasesUpdatedListener): BillingClient {
    return BillingClient.newBuilder(activity).setListener(updateListener).build()
  }
}