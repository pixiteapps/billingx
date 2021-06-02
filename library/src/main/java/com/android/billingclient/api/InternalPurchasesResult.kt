package com.android.billingclient.api

class InternalPurchasesResult(
        responseCode: Int,
        purchasesList: List<Purchase>?
) : Purchase.PurchasesResult(
        BillingResult.newBuilder().setResponseCode(responseCode).build(),
        purchasesList
)