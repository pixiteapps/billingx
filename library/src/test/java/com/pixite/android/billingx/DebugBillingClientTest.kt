package com.pixite.android.billingx

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.InternalPurchasesResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.io.IOException
import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.Executor

class DebugBillingClientTest {

  lateinit var application: Application
  lateinit var activity: Activity
  lateinit var store: BillingStore
  lateinit var purchasesUpdatedListener: FakePurchasesUpdatedListener
  lateinit var client: DebugBillingClient
  private val localBroadcastInteractor = object : LocalBroadcastInteractor {
    override fun registerReceiver(context: Context, broadcastReceiver: BroadcastReceiver, intentFilter: IntentFilter) {
      // no op
    }
    override fun unregisterReceiver(context: Context, broadcastReceiver: BroadcastReceiver) {
      // no op
    }
  }

  private val emptyStateListener = object : BillingClientStateListener {
    override fun onBillingServiceDisconnected() {}
    override fun onBillingSetupFinished(billingResult: BillingResult) {}
  }

  private fun getJson(jsonFileName: String): String {
    return try {
      val inputStream = javaClass.classLoader?.getResourceAsStream(jsonFileName) ?: return ""
      val size = inputStream.available()
      val buffer = ByteArray(size)
      inputStream.read(buffer)
      inputStream.close()
      String(buffer, Charset.forName("UTF-8"))
    } catch (e: IOException) {
      ""
    }
  }
  private fun String.replacePurchaseTime(): String =
          replace("purchase_time", "${Date().time}")

  private val subsPurchase1Json = getJson("subs_purchase_1.json").replacePurchaseTime()
  private val subsPurchase1 =
          Purchase(subsPurchase1Json, "debug-signature-com.foo.package.sku-subs")
  private val subsPurchaseHistoryRecord1 =
          PurchaseHistoryRecord(subsPurchase1Json, "debug-signature-com.foo.package.sku-subs")
  private val subsPurchase2Json = getJson("subs_purchase_2.json").replacePurchaseTime()
  private val subsPurchase2 =
          Purchase(subsPurchase2Json, "debug-signature-com.bar.package.sku-subs")
  private val subsPurchaseHistoryRecord2 =
          PurchaseHistoryRecord(subsPurchase2Json, "debug-signature-com.bar.package.sku-subs")

  private val inappPurchase1Json = getJson("in_app_purchase_1.json").replacePurchaseTime()
  private val inappPurchase1 =
          Purchase(inappPurchase1Json, "debug-signature-com.foo.package.sku-inapp")
  private val inappPurchaseHistoryRecord1 =
          PurchaseHistoryRecord(inappPurchase1Json, "debug-signature-com.foo.package.sku-inapp")
  private val inappPurchase2Json = getJson("in_app_purchase_2.json").replacePurchaseTime()
  private val inappPurchase2 =
          Purchase(inappPurchase2Json, "debug-signature-com.bar.package.sku-inapp")
  private val inappPurchaseHistoryRecord2 =
          PurchaseHistoryRecord(inappPurchase2Json, "debug-signature-com.foo.package.sku-inapp")

  private val subs1Json = "{\"productId\":\"com.foo.package.sku\",\"type\":\"" +
      "subs\",\"price\":\"\$4.99\",\"price_amount_micros\":\"4990000\",\"price_currency_code\":\"" +
      "USD\",\"title\":\"Foo\",\"description\":\"So much Foo\",\"subscriptionPeriod\":\"P1W\",\"f" +
      "reeTrialPeriod\":\"P1W\"}"
  private val subs2Json = "{\"productId\":\"com.bar.package.sku\",\"type\":\"" +
      "subs\",\"price\":\"\$9.99\",\"price_amount_micros\":\"9990000\",\"price_currency_code\":\"" +
      "USD\",\"title\":\"Bar\",\"description\":\"So much Bar\",\"subscriptionPeriod\":\"P1M\"}"
  private val inapp1Json = "{\"productId\":\"com.baz.package.sku\",\"type\":\"" +
      "inapp\",\"price\":\"\$14.99\",\"price_amount_micros\":\"14990000\",\"price_currency_code\":" +
      "\"USD\",\"title\":\"Baz\",\"description\":\"So much Baz\"}"
  private val subs1 = SkuDetails(subs1Json)
  private val subs2 = SkuDetails(subs2Json)
  private val inapp1 = SkuDetails(inapp1Json)

  private var connectionState: Int = BillingClient.ConnectionState.DISCONNECTED

  @Before fun setup() {
    application = mock()
    activity = mock {
      on { applicationContext } doReturn application
    }
    store = mock {
      on { getPurchases(eq(SkuType.INAPP)) } doReturn InternalPurchasesResult(BillingResponseCode.OK, listOf(inappPurchase1, inappPurchase2))
      on { getPurchases(eq(SkuType.SUBS)) } doReturn InternalPurchasesResult(BillingResponseCode.OK, listOf(subsPurchase1, subsPurchase2))
      on { getPurchaseHistoryRecords(eq(SkuType.INAPP)) } doReturn listOf(inappPurchaseHistoryRecord1, inappPurchaseHistoryRecord2)
      on { getPurchaseHistoryRecords(eq(SkuType.SUBS)) } doReturn listOf(subsPurchaseHistoryRecord1, subsPurchaseHistoryRecord2)
      on { getSkuDetails(any()) } doAnswer {
        val params = it.arguments.first() as SkuDetailsParams
        when (params.skuType) {
          SkuType.SUBS -> listOf(subs1, subs2)
          SkuType.INAPP -> listOf(inapp1)
          else -> throw IllegalArgumentException("Unknown skuType: ${params.skuType}")
        }.filter { it.sku in params.skusList }
      }
      on { setConnectionState(any()) } doAnswer {
        connectionState = it.arguments.first() as Int
        this.mock
      }
      on { getConnectionState() } doAnswer { connectionState }
    }
    purchasesUpdatedListener = FakePurchasesUpdatedListener()
    client = DebugBillingClient(activity, purchasesUpdatedListener,
        Executor { command -> command?.run() }, store, localBroadcastInteractor, VoidLogger())
  }

  @Test fun `refuses to start a new connection after the connection is closed`() {
    var response = -1
    val responseListener = object : BillingClientStateListener {
      override fun onBillingServiceDisconnected() {}

      override fun onBillingSetupFinished(billingResult: BillingResult) {
        response = billingResult.responseCode
      }
    }
    client.startConnection(responseListener)
    assertThat(client.isReady).isTrue()

    client.endConnection()

    client.startConnection(responseListener)
    assertThat(response).isEqualTo(BillingResponseCode.DEVELOPER_ERROR)
  }

  @Test fun methodsFailWithoutStartedConnection() {
    assertThat(client.isReady).isFalse()
    assertThat(client.isFeatureSupported(FeatureType.SUBSCRIPTIONS).responseCode)
        .isEqualTo(BillingResponseCode.SERVICE_DISCONNECTED)

    // connect
    client.startConnection(emptyStateListener)

    assertThat(client.isReady).isTrue()
    assertThat(client.isFeatureSupported(FeatureType.SUBSCRIPTIONS).responseCode)
        .isEqualTo(BillingResponseCode.OK)
  }

  @Test fun queryPurchasesReturnsDisconnected() {
    val response = client.queryPurchases("com.foo")
    assertThat(response.responseCode).isEqualTo(BillingResponseCode.SERVICE_DISCONNECTED)
    assertThat(response.purchasesList).isNull()
  }

  @Test fun queryPurchasesReturnsSavedSubscriptions() {
    client.startConnection(emptyStateListener)

    val response = client.queryPurchases(SkuType.SUBS)
    assertThat(response.responseCode).isEqualTo(BillingResponseCode.OK)
    assertThat(response.purchasesList?.get(0)).isEqualTo(subsPurchase1)
    assertThat(response.purchasesList?.get(1)).isEqualTo(subsPurchase2)
  }

  @Test fun queryPurchasesReturnsSavedInAppPurchases() {
    client.startConnection(emptyStateListener)

    val response = client.queryPurchases(SkuType.INAPP)
    assertThat(response.responseCode).isEqualTo(BillingResponseCode.OK)
    assertThat(response.purchasesList?.get(0)).isEqualTo(inappPurchase1)
    assertThat(response.purchasesList?.get(1)).isEqualTo(inappPurchase2)
  }

  @Test fun querySkuDetailsAsyncReturnsDisconnected() {
    val listener = FakeSkuDetailsResponseListener()
    client.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
        .setType(SkuType.SUBS).setSkusList(listOf()).build(), listener)

    assertThat(listener.responseCode).isEqualTo(BillingResponseCode.SERVICE_DISCONNECTED)
    assertThat(listener.skuDetailsList).isNull()
  }

  @Test fun querySkuDetailsAsyncReturnsSavedSubscriptions() {
    val listener = FakeSkuDetailsResponseListener()
    client.startConnection(emptyStateListener)
    client.querySkuDetailsAsync(
        SkuDetailsParams.newBuilder()
            .setType(SkuType.SUBS)
            .setSkusList(listOf(subs1.sku, subs2.sku))
            .build(),
        listener)

    assertThat(listener.responseCode).isEqualTo(BillingResponseCode.OK)
    assertThat(listener.skuDetailsList).containsExactlyElementsIn(listOf(subs1, subs2))
  }

  @Test fun querySkuDetailsAsyncReturnsSavedInAppProducts() {
    val listener = FakeSkuDetailsResponseListener()
    client.startConnection(emptyStateListener)
    client.querySkuDetailsAsync(
        SkuDetailsParams.newBuilder()
            .setType(SkuType.INAPP)
            .setSkusList(listOf(inapp1.sku))
            .build(),
        listener)

    assertThat(listener.responseCode).isEqualTo(BillingResponseCode.OK)
    assertThat(listener.skuDetailsList).containsExactlyElementsIn(listOf(inapp1))
  }

  @Test fun queryPurchaseHistoryAsyncReturnsDisconnected() {
    val listener = FakePurchaseHistoryResponseListener()
    client.queryPurchaseHistoryAsync(SkuType.SUBS, listener)
    assertThat(listener.responseCode).isEqualTo(BillingResponseCode.SERVICE_DISCONNECTED)
    assertThat(listener.purchaseHistoryRecordList).isNull()
  }

  @Test fun queryPurchaseHistoryAsyncReturnsSavedSubscriptions() {
    client.startConnection(emptyStateListener)

    val listener = FakePurchaseHistoryResponseListener()
    client.queryPurchaseHistoryAsync(SkuType.SUBS, listener)
    assertThat(listener.responseCode).isEqualTo(BillingResponseCode.OK)
    assertThat(listener.purchaseHistoryRecordList)
            .containsExactlyElementsIn(listOf(subsPurchaseHistoryRecord1, subsPurchaseHistoryRecord2))
  }

  @Test fun queryPurchaseHistoryAsyncReturnsSavedInAppProducts() {
    client.startConnection(emptyStateListener)

    val listener = FakePurchaseHistoryResponseListener()
    client.queryPurchaseHistoryAsync(SkuType.INAPP, listener)
    assertThat(listener.responseCode).isEqualTo(BillingResponseCode.OK)
    assertThat(listener.purchaseHistoryRecordList)
            .containsExactlyElementsIn(listOf(inappPurchaseHistoryRecord1, inappPurchaseHistoryRecord2))
  }

  class FakeSkuDetailsResponseListener : SkuDetailsResponseListener {
    var responseCode: Int? = null
    var skuDetailsList: List<SkuDetails>? = null
    override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: MutableList<SkuDetails>?) {
      this.responseCode = billingResult.responseCode
      this.skuDetailsList = skuDetailsList
    }
  }

  class FakePurchaseHistoryResponseListener : PurchaseHistoryResponseListener {
    var responseCode: Int? = null
    var purchaseHistoryRecordList: MutableList<PurchaseHistoryRecord>? = null
    override fun onPurchaseHistoryResponse(billingResult: BillingResult, purchaseHistoryRecord: MutableList<PurchaseHistoryRecord>?) {
      this.responseCode = billingResult.responseCode
      this.purchaseHistoryRecordList = purchaseHistoryRecord
    }
  }

  class FakePurchasesUpdatedListener : PurchasesUpdatedListener {
    var responseCode: Int? = null
    var purchasesList: MutableList<Purchase>? = null
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
      this.responseCode = billingResult.responseCode
      this.purchasesList = purchases
    }
  }

  class VoidLogger : BillingLogger {
    override fun v(msg: String) {}
    override fun w(msg: String) {}
  }
}