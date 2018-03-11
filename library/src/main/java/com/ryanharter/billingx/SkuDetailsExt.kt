package com.ryanharter.billingx

import com.android.billingclient.api.SkuDetails
import org.json.JSONObject

val SkuDetails.originalJson: String
  get() = JSONObject().apply {
    put("productId", sku)
    put("type", type)
    put("price", price)
    put("price_amount_micros", priceAmountMicros)
    put("price_currency_code", priceCurrencyCode)
    put("title", title)
    put("description", description)

    subscriptionPeriod?.let { put("subscriptionPeriod", subscriptionPeriod) }
    freeTrialPeriod?.let { put("freeTrialPeriod", freeTrialPeriod) }
    introductoryPrice?.let { put("introductoryPrice", introductoryPrice) }
    introductoryPriceAmountMicros?.let { put("introductoryPriceAmountMicros", introductoryPriceAmountMicros) }
    introductoryPricePeriod?.let { put("introductoryPricePeriod", introductoryPricePeriod) }
    introductoryPriceCycles?.let { put("introductoryPriceCycles", introductoryPriceCycles) }
  }.toString()