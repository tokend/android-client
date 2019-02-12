package org.tokend.template.data.model.history.details

import org.tokend.sdk.api.generated.resources.EffectMatchedResource
import java.math.BigDecimal

class InvestmentDetails(
        offerId: Long,
        orderBookId: Long,
        price: BigDecimal,
        quoteAssetCode: String,
        charged: ParticularBalanceChangeDetails,
        funded: ParticularBalanceChangeDetails
) : OfferMatchDetails(offerId, orderBookId, price, true, quoteAssetCode, charged, funded) {
    constructor(effect: EffectMatchedResource) : this(
            offerId = effect.offerId,
            orderBookId = effect.orderBookId,
            price = effect.price,
            quoteAssetCode = effect.charged.assetCode, // Investments always charge in quote asset
            charged = ParticularBalanceChangeDetails(effect.charged),
            funded = ParticularBalanceChangeDetails(effect.funded)
    )
}