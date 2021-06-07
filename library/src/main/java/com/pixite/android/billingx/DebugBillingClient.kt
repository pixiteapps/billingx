package com.pixite.android.billingx

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.InternalPurchasesResult
import com.android.billingclient.api.PriceChangeConfirmationListener
import com.android.billingclient.api.PriceChangeFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.pixite.android.billingx.DebugBillingClient.ClientState.CLOSED
import com.pixite.android.billingx.DebugBillingClient.ClientState.CONNECTED
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
        val resultData = intent?.getBundleExtra(DebugBillingActivity.RESPONSE_BUNDLE)?.let {
          val purchaseDataList = it.getStringArrayList(DebugBillingActivity.RESPONSE_INAPP_PURCHASE_DATA_LIST).orEmpty()
          val signatureList = it.getStringArrayList(DebugBillingActivity.RESPONSE_INAPP_SIGNATURE_LIST).orEmpty()
          purchaseDataList.mapIndexed { index, purchaseData ->
              Purchase(purchaseData, signatureList[index])
          }.toList()
        }.orEmpty()
        purchases = resultData

        // save the purchase
        purchases.forEach { billingStore.addPurchase(it) }
      }

      // save the result
      purchasesUpdatedListener.onPurchasesUpdated(createBillingResult(responseCode), purchases)
    }
  }
  @BillingResponseCode
  private fun Int.toBillingResult(): BillingResult {
    return BillingResult.newBuilder().setResponseCode(this).build()
  }

  private fun createBillingResult(@BillingResponseCode billingResponseCode: Int): BillingResult {
    return BillingResult.newBuilder().setResponseCode(billingResponseCode).build()
  }

  override fun isReady(): Boolean = clientState == CONNECTED

  override fun startConnection(listener: BillingClientStateListener) {
    if (isReady) {
      listener.onBillingSetupFinished(BillingResponseCode.OK.toBillingResult())
      return
    }

    if (clientState == CLOSED) {
      logger.w("Client was already closed and can't be reused. Please create another instance.")
      listener.onBillingSetupFinished(BillingResponseCode.DEVELOPER_ERROR.toBillingResult())
      return
    }

    localBroadcastInteractor.registerReceiver(
        context, broadcastReceiver,
        IntentFilter(DebugBillingActivity.RESPONSE_INTENT_ACTION)
    )
    this.billingClientStateListener = listener
    clientState = CONNECTED
    listener.onBillingSetupFinished(BillingResponseCode.OK.toBillingResult())
  }

  override fun endConnection() {
    localBroadcastInteractor.unregisterReceiver(context, broadcastReceiver)
    billingClientStateListener?.onBillingServiceDisconnected()
    clientState = CLOSED
  }

  override fun isFeatureSupported(feature: String): BillingResult {
    // TODO Update BillingStore to allow feature enable/disable
    return if (!isReady) {
      BillingResponseCode.SERVICE_DISCONNECTED.toBillingResult()
    } else {
      BillingResponseCode.OK.toBillingResult()
    }
  }

  override fun consumeAsync(consumeParams: ConsumeParams, listener: ConsumeResponseListener) {
    if (consumeParams.purchaseToken.isBlank()) {
      listener.onConsumeResponse(BillingResponseCode.DEVELOPER_ERROR.toBillingResult(), consumeParams.purchaseToken.orEmpty())
      return
    }

    backgroundExecutor.execute {
      val purchase = billingStore.getPurchaseByToken(consumeParams.purchaseToken)
      if (purchase != null) {
        billingStore.removePurchase(purchase.purchaseToken)
        listener.onConsumeResponse(BillingResponseCode.OK.toBillingResult(), consumeParams.purchaseToken)
      } else {
        listener.onConsumeResponse(BillingResponseCode.ITEM_NOT_OWNED.toBillingResult(), consumeParams.purchaseToken)
      }
    }
  }

  override fun launchBillingFlow(activity: Activity, params: BillingFlowParams): BillingResult {
    val intent = Intent(activity, DebugBillingActivity::class.java)
    val skuDetails: List<SkuDetails> = params.zzj()
    val skuDetailsJson: Array<String> = skuDetails.map { it.originalJson }.toTypedArray()
    intent.putExtra(DebugBillingActivity.REQUEST_SKU_DETAILS, skuDetailsJson)
    activity.startActivity(intent)
    return BillingResponseCode.OK.toBillingResult()
  }

  override fun queryPurchaseHistoryAsync(
      skuType: String, listener: PurchaseHistoryResponseListener
  ) {
    if (!isReady) {
      listener.onPurchaseHistoryResponse(BillingResponseCode.SERVICE_DISCONNECTED.toBillingResult(), null)
      return
    }
    backgroundExecutor.execute {
      val history = queryPurchases(skuType)
      listener.onPurchaseHistoryResponse(
              history.responseCode.toBillingResult(),
              billingStore.getPurchaseHistoryRecords(skuType)
      )
    }
  }

  override fun querySkuDetailsAsync(
      params: SkuDetailsParams, listener: SkuDetailsResponseListener
  ) {
    if (!isReady) {
      listener.onSkuDetailsResponse(BillingResponseCode.SERVICE_DISCONNECTED.toBillingResult(), null)
      return
    }
    backgroundExecutor.execute {
      listener.onSkuDetailsResponse(BillingResponseCode.OK.toBillingResult(), billingStore.getSkuDetails(params))
    }
  }

  override fun queryPurchases(@SkuType skuType: String): Purchase.PurchasesResult {
    if (!isReady) {
      return InternalPurchasesResult(BillingResponseCode.SERVICE_DISCONNECTED, null)
    }
    if (skuType.isBlank()) {
      logger.w("Please provide a valid SKU type.")
      return InternalPurchasesResult(BillingResponseCode.DEVELOPER_ERROR, /* purchasesList */ null)
    }
    return billingStore.getPurchases(skuType)
  }

  override fun launchPriceChangeConfirmationFlow(
          activity: Activity,
          priceChangeFlowParams: PriceChangeFlowParams,
          priceChangeConfirmationListener: PriceChangeConfirmationListener) {
    throw NotImplementedError("This method is not supported")
  }

  override fun acknowledgePurchase(
          acknowledgePurchaseParams: AcknowledgePurchaseParams,
          acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener
  ) {
      billingStore.acknowledgePurchase(acknowledgePurchaseParams.purchaseToken)
      acknowledgePurchaseResponseListener
              .onAcknowledgePurchaseResponse(BillingResponseCode.OK.toBillingResult())
  }

  override fun getConnectionState(): Int {
    TODO("Not yet implemented")
  }

  override fun queryPurchasesAsync(skuType: String, listener: PurchasesResponseListener) {
    TODO("Not yet implemented")
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