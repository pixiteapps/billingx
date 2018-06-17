package com.pixite.billingx

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.TextView
import com.pixite.billingx.util.bindView

class MainActivity : BaseActivity() {

  private val purchaseButton: FloatingActionButton by bindView(R.id.purchase_button)
  private val statusText: TextView by bindView(R.id.status_text)

  private val subsRepo by lazy(LazyThreadSafetyMode.NONE) { injection().subscriptionRepository }
  private val billingManager by lazy(LazyThreadSafetyMode.NONE) { injection().billingManager(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    subsRepo.subscribed.observe(this, Observer {
      if (it == true) {
        purchaseButton.visibility = View.GONE
        statusText.setText(R.string.status_subscribed)
      } else {
        purchaseButton.visibility = View.VISIBLE
        statusText.setText(R.string.status_not_subscribed)
      }
    })

    findViewById<FloatingActionButton>(R.id.purchase_button).setOnClickListener {
      billingManager.initiatePurchase(BillingManager.SKU_SUBS)
    }
  }

}