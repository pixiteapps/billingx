package com.pixite.billingx

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AppExecutors(val diskIO: Executor, val networkIO: Executor,
                   val workThread: Executor, val mainThread: Executor) {

  constructor() : this(
      Executors.newSingleThreadExecutor(),
      Executors.newFixedThreadPool(3),
      Executors.newFixedThreadPool(6),
      MainThreadExecutor()
  )
}

class MainThreadExecutor : Executor {
  private val mainThreadHandler = Handler(Looper.getMainLooper())
  override fun execute(command: Runnable?) {
      command?.let {
          mainThreadHandler.post(command)
      }
  }
}