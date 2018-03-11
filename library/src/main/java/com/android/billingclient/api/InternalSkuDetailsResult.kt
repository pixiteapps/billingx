package com.android.billingclient.api

internal class InternalSkuDetailsResult(responseCode: Int, skuDetailsList: List<SkuDetails>?)
  : SkuDetails.SkuDetailsResult(responseCode, skuDetailsList)