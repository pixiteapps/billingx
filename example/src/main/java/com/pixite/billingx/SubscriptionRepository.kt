package com.pixite.billingx

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

class SubscriptionRepository {

  private val _subscribed = MutableLiveData<Boolean>().apply { value = false }
  val subscribed: LiveData<Boolean> = _subscribed

  fun setSubscribed(subscribed: Boolean) {
    _subscribed.postValue(subscribed)
  }

}