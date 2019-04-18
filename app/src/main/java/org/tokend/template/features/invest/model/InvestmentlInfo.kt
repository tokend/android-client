package org.tokend.template.features.invest.model

import org.tokend.template.data.model.OfferRecord

class InvestmentlInfo(
        /**
         * Pending offers by quote assets
         */
        val offersByAsset: Map<String, OfferRecord>,
        /**
         * Detailed sale info contains calculated caps for quote assets
         */
        val detailedSale: SaleRecord
)