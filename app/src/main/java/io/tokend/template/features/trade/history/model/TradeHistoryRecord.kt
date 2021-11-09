package io.tokend.template.features.trade.history.model

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import org.tokend.sdk.api.generated.resources.MatchResource
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class TradeHistoryRecord(
    val id: Long,
    val baseAsset: Asset,
    val quoteAsset: Asset,
    val baseAmount: BigDecimal,
    val quoteAmount: BigDecimal,
    val price: BigDecimal,
    val createdAt: Date,
    var hasPositiveTrend: Boolean
) : Serializable {
    constructor(source: MatchResource) : this(
        id = source.id.toLong(),
        baseAsset = SimpleAsset(source.baseAsset),
        quoteAsset = SimpleAsset(source.quoteAsset),
        baseAmount = source.baseAmount,
        quoteAmount = source.quoteAmount,
        price = source.price,
        createdAt = source.createdAt,
        hasPositiveTrend = true
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TradeHistoryRecord

        if (id != other.id) return false
        if (hasPositiveTrend != other.hasPositiveTrend) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + hasPositiveTrend.hashCode()
        return result
    }
}