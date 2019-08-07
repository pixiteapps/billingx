package com.android.billingclient.api

class InternalPurchasesResult(responseCode: BillingResult, purchasesList: List<Purchase>?)
  : Purchase.PurchasesResult(responseCode, purchasesList)