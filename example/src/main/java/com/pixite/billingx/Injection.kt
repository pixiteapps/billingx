package com.pixite.billingx

import android.content.Context
import androidx.fragment.app.FragmentActivity

fun Context.injection() = Injection.getInstance(this.applicationContext)

class Injection {

  val appExecutors by lazy(LazyThreadSafetyMode.NONE) { AppExecutors() }

  val subscriptionRepository by lazy(LazyThreadSafetyMode.NONE) { SubscriptionRepository() }

  private val billingClientFactory by lazy(LazyThreadSafetyMode.NONE) { BillingClientFactory() }

  fun billingManager(activity: FragmentActivity): BillingManager {
    return BillingManager(activity, billingClientFactory, subscriptionRepository, appExecutors)
  }

  companion object {
    private var INSTANCE: Injection? = null
    private val lock = Any()
    fun getInstance(context: Context): Injection {
      if (INSTANCE == null) {
        synchronized(lock) {
          if (INSTANCE == null) {
            INSTANCE = Injection()
          }
        }
      }
      return INSTANCE!!
    }
  }

}