package com.android.billingclient.api

internal class InternalSkuDetailsResult(responseCode: Int, debugMessage: String, skuDetailsList: List<SkuDetails>?)
  : SkuDetails.SkuDetailsResult(responseCode, debugMessage, skuDetailsList)