package com.ryanharter.billingx.debug

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import com.ryanharter.android.billingx.BillingStore
import com.ryanharter.android.billingx.PurchaseBuilder
import com.ryanharter.billingx.BillingManager
import com.ryanharter.billingx.R
import com.ryanharter.billingx.SubscriptionRepository
import com.ryanharter.billingx.injection
import com.ryanharter.billingx.util.bindView
import java.util.Date

class DebugDrawer : Fragment() {

  private val subscriptionSwitch: Switch by bindView(R.id.subscription)

  private val subsRepo: SubscriptionRepository by lazy(LazyThreadSafetyMode.NONE) {
    activity!!.injection().subscriptionRepository
  }

  private var autoSwitch: Boolean = false

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.debug_drawer, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    subsRepo.subscribed.observe(this, Observer {
      autoSwitch = true
      subscriptionSwitch.isChecked = it == true
    })

    subscriptionSwitch.setOnCheckedChangeListener { _, isChecked ->
      if (autoSwitch) {
        // Handles cases where the switch was flipped outside of our control
        autoSwitch = false
        return@setOnCheckedChangeListener
      }

      val ctx = context ?: return@setOnCheckedChangeListener
      if (isChecked) {
        BillingStore.getInstance(ctx)
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
        BillingStore.getInstance(ctx).clearPurchases()
        subsRepo.setSubscribed(false)
      }
    }
  }

}