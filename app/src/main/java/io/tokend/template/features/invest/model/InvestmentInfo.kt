package io.tokend.template.features.invest.model

import io.tokend.template.features.offers.model.OfferRecord

class InvestmentInfo(
    /**
     * Pending offers by quote assets
     */
    val offersByAsset: Map<String, OfferRecord>,
    /**
     * Detailed sale info contains calculated caps for quote assets
     */
    val detailedSale: SaleRecord
)