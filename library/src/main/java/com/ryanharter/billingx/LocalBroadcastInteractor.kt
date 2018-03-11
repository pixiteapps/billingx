package com.ryanharter.billingx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager

interface LocalBroadcastInteractor {
  fun registerReceiver(context: Context, broadcastReceiver: BroadcastReceiver, intentFilter: IntentFilter)
}

class AndroidLocalBroadcastInteractor : LocalBroadcastInteractor {
  override fun registerReceiver(context: Context, broadcastReceiver: BroadcastReceiver, intentFilter: IntentFilter) {
    LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter)
  }
}