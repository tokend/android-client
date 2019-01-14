package org.tokend.template.data.model

import org.tokend.sdk.api.trades.model.Offer
import org.tokend.wallet.Base32Check
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class OfferRecord(
        val baseAssetCode: String,
        val quoteAssetCode: String,
        val price: BigDecimal,
        val isBuy: Boolean,
        val baseAmount: BigDecimal,
        val quoteAmount: BigDecimal = baseAmount * price,
        val id: Long = 0L,
        val orderBookId: Long = 0L,
        val date: Date = Date(),
        var baseBalanceId: String = EMPTY_BALANCE_ID,
        var quoteBalanceId: String = EMPTY_BALANCE_ID,
        var fee: BigDecimal = BigDecimal.ZERO,
        val pagingToken: String = "0"
) : Serializable {
    constructor(source: Offer) : this(
            id = source.id,
            orderBookId = source.orderBookId,
            baseAssetCode = source.baseAsset,
            quoteAssetCode = source.quoteAsset,
            price = source.price,
            isBuy = source.isBuy,
            date = source.date,
            baseBalanceId = source.baseBalance ?: EMPTY_BALANCE_ID,
            quoteBalanceId = source.quoteBalance ?: EMPTY_BALANCE_ID,
            baseAmount = source.baseAmount,
            quoteAmount = source.quoteAmount,
            fee = source.fee ?: BigDecimal.ZERO,
            pagingToken = source.pagingToken
    )

    override fun equals(other: Any?): Boolean {
        return other is OfferRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun toOffer(): Offer {
        return Offer(
                baseAmount = baseAmount,
                baseAsset = baseAssetCode,
                quoteAmount = quoteAmount,
                quoteAsset = quoteAssetCode,
                isBuy = isBuy,
                date = date,
                pagingToken = pagingToken,
                fee = fee,
                id = id,
                orderBookId = orderBookId,
                price = price,
                baseBalance = baseBalanceId,
                quoteBalance = quoteBalanceId
        )
    }

    companion object {
        val EMPTY_BALANCE_ID = Base32Check.encodeBalanceId(byteArrayOf(32))
    }
}