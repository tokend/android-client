package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.EffectMatchedResource
import org.tokend.sdk.api.generated.resources.OpManageOfferDetailsResource
import java.math.BigDecimal

class OfferMatchDetails(
        val offerId: Long,
        val orderBookId: Long,
        val price: BigDecimal,
        val isBuy: Boolean,
        val quoteAssetCode: String,
        val charged: ParticularBalanceChangeDetails,
        val funded: ParticularBalanceChangeDetails

): BalanceChangeDetails() {
    constructor(op: OpManageOfferDetailsResource,
                effect: EffectMatchedResource): this(
            offerId = op.offerId,
            orderBookId = op.orderBookId,
            price = op.price,
            isBuy = op.isBuy,
            quoteAssetCode = op.quoteAsset.id,
            charged = ParticularBalanceChangeDetails(effect.charged),
            funded = ParticularBalanceChangeDetails(effect.funded)
    )

    val isPrimaryMarket = orderBookId != 0L
}