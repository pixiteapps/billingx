package com.pixite.billingx.debug

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import com.pixite.android.billingx.BillingStore
import com.pixite.android.billingx.PurchaseBuilder
import com.pixite.billingx.BillingManager
import com.ryanharter.billingx.R
import com.pixite.billingx.SubscriptionRepository
import com.pixite.billingx.injection
import com.pixite.billingx.util.bindView
import java.util.Date

class DebugDrawer : Fragment() {

  private val subscriptionSwitch: Switch by bindView(R.id.subscription)

  private val subsRepo: SubscriptionRepository by lazy(LazyThreadSafetyMode.NONE) {
    activity!!.injection().subscriptionRepository
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.debug_drawer, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    subsRepo.subscribed.observe(this, Observer {
      subscriptionSwitch.isChecked = it == true
    })

    subscriptionSwitch.setOnClickListener {
      val ctx = context ?: return@setOnClickListener
      if (subscriptionSwitch.isChecked) {
        BillingStore.defaultStore(ctx)
            .addPurchase(
                PurchaseBuilder(
                    orderId = "abcd123",
                    packageName = ctx.packageName,
                    sku = BillingManager.SKU_SUBS,
                    purchaseTime = Date().time,
                    purchaseToken = "token-123",
                    signature = "foo",
                    isAutoRenewing = true
                ).build()
            )
        subsRepo.setSubscribed(true)
      } else {
        BillingStore.defaultStore(ctx).clearPurchases()
        subsRepo.setSubscribed(false)
      }
    }
  }

}