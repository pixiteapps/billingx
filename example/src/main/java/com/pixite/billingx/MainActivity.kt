package com.pixite.billingx

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : BaseActivity() {

  private lateinit var purchaseButton: FloatingActionButton
  private lateinit var statusText: TextView

  private val subsRepo by lazy(LazyThreadSafetyMode.NONE) { injection().subscriptionRepository }
  private val billingManager by lazy(LazyThreadSafetyMode.NONE) { injection().billingManager(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    purchaseButton = findViewById(R.id.purchase_button)
    statusText = findViewById(R.id.status_text)

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