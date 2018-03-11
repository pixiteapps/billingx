package com.ryanharter.billingx

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.InternalPurchasesResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.android.billingclient.util.BillingHelper
import java.util.concurrent.Executor

class DebugBillingClient(private val activity: Activity,
                         private val backgroundExecutor: Executor,
                         private val purchasesUpdatedListener: PurchasesUpdatedListener,
                         private val billingStore: BillingStore = BillingStore.getInstance(activity),
                         private val localBroadcastInteractor: LocalBroadcastInteractor = AndroidLocalBroadcastInteractor()) : BillingClient() {

  companion object {
    private const val TAG = "DebugBillingClient"
  }

  private var billingClientStateListener: BillingClientStateListener? = null
  private var connected = false

  private val broadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      // Receiving the result from local broadcast and triggering a callback on listener.
      @BillingResponse
      val responseCode = intent?.getIntExtra(DebugBillingActivity.RESPONSE_CODE, BillingResponse.ERROR) ?: BillingResponse.ERROR
      val resultData = intent?.getBundleExtra(DebugBillingActivity.RESPONSE_BUNDLE)
      val purchases = BillingHelper.extractPurchases(resultData)

      // save the purchase
      purchases.forEach { billingStore.addPurchase(it) }

      // save the result
      purchasesUpdatedListener.onPurchasesUpdated(responseCode, purchases)
    }
  }

  override fun isReady(): Boolean = connected

  override fun startConnection(listener: BillingClientStateListener) {
    localBroadcastInteractor.registerReceiver(activity, broadcastReceiver,
        IntentFilter(DebugBillingActivity.RESPONSE_INTENT_ACTION))
    connected = true
    this.billingClientStateListener = listener
    listener.onBillingSetupFinished(BillingResponse.OK)
  }

  override fun endConnection() {
    billingClientStateListener?.onBillingServiceDisconnected()
    connected = false
  }

  override fun isFeatureSupported(feature: String?): Int {
    return if (!connected) {
      BillingResponse.SERVICE_DISCONNECTED
    } else {
      BillingResponse.OK
    }
  }

  override fun consumeAsync(purchaseToken: String?, listener: ConsumeResponseListener?) {
    TODO("not implemented")
  }

  override fun launchBillingFlow(activity: Activity?, params: BillingFlowParams?): Int {
    val intent = Intent(activity, DebugBillingActivity::class.java)
    intent.putExtra(DebugBillingActivity.REQUEST_SKU_TYPE, params?.skuType)
    intent.putExtra(DebugBillingActivity.REQUEST_SKU, params?.sku)
    activity!!.startActivity(intent)
    return BillingResponse.OK
  }

  override fun queryPurchaseHistoryAsync(skuType: String?, listener: PurchaseHistoryResponseListener?) {
    if (!isReady) {
      listener?.onPurchaseHistoryResponse(BillingResponse.SERVICE_DISCONNECTED, null)
      return
    }
    backgroundExecutor.execute {
      val history = queryPurchases(skuType)
      listener?.onPurchaseHistoryResponse(history.responseCode, history.purchasesList)
    }
  }

  override fun querySkuDetailsAsync(params: SkuDetailsParams, listener: SkuDetailsResponseListener?) {
    if (!isReady) {
      listener?.onSkuDetailsResponse(BillingResponse.SERVICE_DISCONNECTED, null)
      return
    }
    backgroundExecutor.execute {
      listener?.onSkuDetailsResponse(BillingResponse.OK, billingStore.getSkuDetails(params))
    }
  }

  override fun queryPurchases(@SkuType skuType: String?): Purchase.PurchasesResult {
    if (!isReady) {
      return InternalPurchasesResult(BillingResponse.SERVICE_DISCONNECTED, null)
    }
    if (skuType == null || skuType.isBlank()) {
      BillingHelper.logWarn(TAG, "Please provide a valid SKU type.")
      return InternalPurchasesResult(BillingResponse.DEVELOPER_ERROR, /* purchasesList */ null)
    }
    return billingStore.getPurchases(skuType)
  }
}