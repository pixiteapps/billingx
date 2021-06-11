package com.pixite.billingx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SubscriptionRepository {

  private val _subscribed = MutableLiveData<Boolean>().apply { value = false }
  val subscribed: LiveData<Boolean> = _subscribed

  fun setSubscribed(subscribed: Boolean) {
    _subscribed.postValue(subscribed)
  }

}