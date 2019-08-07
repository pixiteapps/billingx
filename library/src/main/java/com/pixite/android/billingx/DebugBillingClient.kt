package com.pixite.android.billingx

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.billingclient.api.*
import com.android.billingclient.util.BillingHelper
import com.pixite.android.billingx.DebugBillingClient.ClientState.CLOSED
import com.pixite.android.billingx.DebugBillingClient.ClientState.CONNECTED
import com.pixite.android.billingx.DebugBillingClient.ClientState.CONNECTING
import com.pixite.android.billingx.DebugBillingClient.ClientState.DISCONNECTED
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DebugBillingClient(
    context: Context,
    private val purchasesUpdatedListener: PurchasesUpdatedListener,
    private val backgroundExecutor: Executor = Executors.newSingleThreadExecutor(),
    private val billingStore: BillingStore = BillingStore.defaultStore(context),
    private val localBroadcastInteractor: LocalBroadcastInteractor =
      AndroidLocalBroadcastInteractor(),
    private val logger: BillingLogger = SimpleBillingLogger()
) : BillingClient() {

  companion object {
    private const val TAG = "DebugBillingClient"

    /**
     * Creates a new DebugBillingClientBuilder for Java consumers.  Kotlin users should prefer
     * the constructor.
     */
    @JvmStatic fun newBuilder(context: Context) = DebugBillingClientBuilder(context)
  }

  private val context = context.applicationContext

  private var billingClientStateListener: BillingClientStateListener? = null

  private enum class ClientState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED
  }

  private var clientState = DISCONNECTED

  private val broadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      // Receiving the result from local broadcast and triggering a callback on listener.
      @BillingResponseCode
      val responseCode =
        intent?.getIntExtra(DebugBillingActivity.RESPONSE_CODE, BillingResponseCode.ERROR)
            ?: BillingResponseCode.ERROR

      var purchases: List<Purchase>? = null
      if (responseCode == BillingResponseCode.OK) {
        val resultData = intent?.getBundleExtra(DebugBillingActivity.RESPONSE_BUNDLE)
        purchases = BillingHelper.extractPurchases(resultData)

        // save the purchase
        purchases.forEach { billingStore.addPurchase(it) }
      }

      // save the result
      purchasesUpdatedListener.onPurchasesUpdated(BillingResult.newBuilder().setResponseCode(responseCode).build(), purchases)
    }
  }

  override fun isReady(): Boolean = clientState == CONNECTED

  override fun startConnection(listener: BillingClientStateListener) {
    if (isReady) {
      listener.onBillingSetupFinished(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build())
      return
    }

    if (clientState == CLOSED) {
      logger.w("Client was already closed and can't be reused. Please create another instance.")
      listener.onBillingSetupFinished(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR).build())
      return
    }

    localBroadcastInteractor.registerReceiver(
        context, broadcastReceiver,
        IntentFilter(DebugBillingActivity.RESPONSE_INTENT_ACTION)
    )
    this.billingClientStateListener = listener
    clientState = CONNECTED
    listener.onBillingSetupFinished(BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build())
  }

  override fun endConnection() {
    localBroadcastInteractor.unregisterReceiver(context, broadcastReceiver)
    billingClientStateListener?.onBillingServiceDisconnected()
    clientState = CLOSED
  }

  override fun isFeatureSupported(feature: String?): BillingResult? {
    // TODO Update BillingStore to allow feature enable/disable
    return if (!isReady) {
      BillingResult.newBuilder().setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED).build()
    } else {
      BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build()
    }
  }

  override fun consumeAsync(consumeParams: ConsumeParams?, listener: ConsumeResponseListener) {
    val purchaseToken = consumeParams?.purchaseToken
    if (purchaseToken == null || purchaseToken.isBlank()) {
      listener.onConsumeResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.DEVELOPER_ERROR).build(), purchaseToken)
      return
    }

    backgroundExecutor.execute {
      val purchase = billingStore.getPurchaseByToken(purchaseToken)
      if (purchase != null) {
        billingStore.removePurchase(purchase.purchaseToken)
        listener?.onConsumeResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build(), purchaseToken)
      } else {
        listener?.onConsumeResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.ITEM_NOT_OWNED).build(), purchaseToken)
      }
    }
  }

  override fun launchBillingFlow(activity: Activity?, params: BillingFlowParams?): BillingResult? {
    val intent = Intent(activity, DebugBillingActivity::class.java)
    intent.putExtra(DebugBillingActivity.REQUEST_SKU_TYPE, params?.skuType)
    intent.putExtra(DebugBillingActivity.REQUEST_SKU, params?.sku)
    activity!!.startActivity(intent)
    return BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build()
  }

  override fun queryPurchaseHistoryAsync(skuType: String?, listener: PurchaseHistoryResponseListener) {
    if (!isReady) {
      listener.onPurchaseHistoryResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED).build(), null)
      return
    }
    backgroundExecutor.execute {
      val history = queryPurchases(skuType)
      listener.onPurchaseHistoryResponse(BillingResult.newBuilder().setResponseCode(history.responseCode).build(),
              history.purchasesList.map { PurchaseHistoryRecord(it.originalJson, it.signature) })
    }
  }

  override fun querySkuDetailsAsync(params: SkuDetailsParams, listener: SkuDetailsResponseListener) {
    if (!isReady) {
      listener.onSkuDetailsResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED).build(), null)
      return
    }
    backgroundExecutor.execute {
      listener.onSkuDetailsResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build(), billingStore.getSkuDetails(params))
    }
  }

  override fun queryPurchases(@SkuType skuType: String?): Purchase.PurchasesResult {
    if (!isReady) {
      return InternalPurchasesResult(BillingResult.newBuilder().setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED).build(), null)
    }
    if (skuType == null || skuType.isBlank()) {
      logger.w("Please provide a valid SKU type.")
      return InternalPurchasesResult(BillingResult.newBuilder().setResponseCode(BillingResponseCode.DEVELOPER_ERROR).build(), /* purchasesList */ null)
    }
    return billingStore.getPurchases(skuType)
  }

  override fun launchPriceChangeConfirmationFlow(activity: Activity?, params: PriceChangeFlowParams?, listener: PriceChangeConfirmationListener) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun loadRewardedSku(params: RewardLoadParams?, listener: RewardResponseListener) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun acknowledgePurchase(params: AcknowledgePurchaseParams?, listener: AcknowledgePurchaseResponseListener?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  // Supplied for easy Java interop.
  class DebugBillingClientBuilder(val context: Context) {
    private lateinit var _listener: PurchasesUpdatedListener
    private var _backgroundExecutor: Executor? = null
    private var _billingStore: BillingStore? = null

    fun setListener(listener: PurchasesUpdatedListener): DebugBillingClientBuilder {
      _listener = listener
      return this
    }

    fun setBackgroundExecutor(backgroundExecutor: Executor): DebugBillingClientBuilder {
      _backgroundExecutor = backgroundExecutor
      return this
    }

    fun setBillingStore(billingStore: BillingStore): DebugBillingClientBuilder {
      _billingStore = billingStore
      return this
    }

    fun build(): DebugBillingClient {
      checkNotNull(_listener, { "listener required" })
      val executor = _backgroundExecutor ?: Executors.newSingleThreadExecutor()
      val store = _billingStore ?: BillingStore.defaultStore(context)
      return DebugBillingClient(context, _listener, executor, store)
    }
  }
}