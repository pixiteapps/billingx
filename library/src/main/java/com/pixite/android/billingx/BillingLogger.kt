package com.pixite.android.billingx

import com.android.billingclient.util.BillingHelper

interface BillingLogger {
  fun v(msg: String)
  fun w(msg: String)
}

class SimpleBillingLogger : BillingLogger {

  override fun v(msg: String) {
    BillingHelper.logVerbose(TAG, msg)
  }

  override fun w(msg: String) {
    BillingHelper.logWarn(TAG, msg)
  }

  companion object {
    private const val TAG = "BillingX"
  }

}