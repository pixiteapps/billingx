package com.android.billingclient.api

class InternalPurchasesResult(responseCode: Int, purchasesList: List<Purchase>?)
  : Purchase.PurchasesResult(responseCode, purchasesList)