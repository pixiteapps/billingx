package com.android.billingclient.api

internal class InternalPurchasesResult(responseCode: Int, purchasesList: List<Purchase>?)
  : Purchase.PurchasesResult(responseCode, purchasesList)