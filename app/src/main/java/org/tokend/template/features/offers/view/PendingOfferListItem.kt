package org.tokend.template.features.offers.view

import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.util.DateProvider
import java.math.BigDecimal
import java.util.*

class PendingOfferListItem(
        val price: BigDecimal,
        val amount: BigDecimal,
        val asset: Asset,
        val isBuy: Boolean,
        val isInvestment: Boolean,
        val counterpartyAsset: Asset,
        override val date: Date,
        val source: OfferRecord? = null
): DateProvider {
    constructor(record: OfferRecord): this(
            price = record.price,
            amount = if (record.isInvestment) record.quoteAmount else record.baseAmount,
            asset = if (record.isInvestment) record.quoteAsset else record.baseAsset,
            isBuy = record.isBuy,
            isInvestment = record.isInvestment,
            counterpartyAsset = if (record.isInvestment) record.baseAsset else record.quoteAsset,
            date = record.date,
            source = record
    )
}