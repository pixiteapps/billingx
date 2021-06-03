package com.pixite.android.billingx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

typealias BroadcastReceiver = (data: Bundle) -> Unit

internal class LocalBroadcastManager(
    private val mainThreadExecutor: Executor = MainThreadExecutor()
) {
  companion object {
    internal var INSTANCE = LocalBroadcastManager()
    fun getInstance(): LocalBroadcastManager = INSTANCE
  }

  private val receivers = mutableMapOf<String, BroadcastReceiver>()

  fun registerReceiver(key: String, receiver: BroadcastReceiver) {
    receivers[key] = receiver
  }

  fun unregisterReceiver(key: String) {
    receivers.remove(key)
  }

  fun broadcast(key: String, data: Bundle) {
    mainThreadExecutor.execute { receivers[key]?.invoke(data) }
  }

  private class MainThreadExecutor : Executor {
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    override fun execute(command: Runnable?) {
      command?.let {
        mainThreadHandler.post(command)
      }
    }
  }

}