package org.tokend.template.features.offers.model

import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.history.SimpleFeeRecord
import java.io.Serializable
import java.math.BigDecimal

class OfferRequest(
        val orderBookId: Long,
        val price: BigDecimal,
        val isBuy: Boolean,
        val baseAsset: Asset,
        val quoteAsset: Asset,
        val baseAmount: BigDecimal,
        val fee: SimpleFeeRecord,
        val offerToCancel: OfferRecord?
) : Serializable {
    val quoteAmount: BigDecimal = baseAmount * price
}