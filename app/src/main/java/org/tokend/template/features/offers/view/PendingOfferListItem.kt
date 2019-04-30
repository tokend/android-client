package org.tokend.template.features.offers.view

import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.util.DateProvider
import java.math.BigDecimal
import java.util.*

class PendingOfferListItem(
        val price: BigDecimal,
        val amount: BigDecimal,
        val assetCode: String,
        val isBuy: Boolean,
        val isInvestment: Boolean,
        val counterpartyAssetCode: String,
        override val date: Date,
        val source: OfferRecord? = null
): DateProvider {
    constructor(record: OfferRecord): this(
            price = record.price,
            amount = if (record.isInvestment) record.quoteAmount else record.baseAmount,
            assetCode = if (record.isInvestment) record.quoteAssetCode else record.baseAssetCode,
            isBuy = record.isBuy,
            isInvestment = record.isInvestment,
            counterpartyAssetCode = if (record.isInvestment) record.baseAssetCode else record.quoteAssetCode,
            date = record.date,
            source = record
    )
}