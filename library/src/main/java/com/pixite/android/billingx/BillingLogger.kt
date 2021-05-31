package com.pixite.android.billingx

import android.util.Log

interface BillingLogger {
  fun v(msg: String)
  fun w(msg: String)
}

class SimpleBillingLogger : BillingLogger {

  override fun v(msg: String) {
    Log.v(TAG, msg)
  }

  override fun w(msg: String) {
    Log.w(TAG, msg)
  }

  companion object {
    private const val TAG = "BillingX"
  }

}