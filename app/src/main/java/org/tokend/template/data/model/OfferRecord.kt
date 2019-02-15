package org.tokend.template.data.model

import org.tokend.sdk.api.generated.resources.OfferResource
import org.tokend.sdk.api.generated.resources.OrderBookEntryResource
import org.tokend.sdk.utils.ApiDateUtil
import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.details.BalanceChangeCause
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
        var fee: BigDecimal = BigDecimal.ZERO
) : Serializable {

    val isInvestment: Boolean
        get() = orderBookId != 0L

    val isCancellable: Boolean
        get() = baseAmount.signum() > 0

    override fun equals(other: Any?): Boolean {
        return other is OfferRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        val EMPTY_BALANCE_ID = Base32Check.encodeBalanceId(byteArrayOf(32))

        @JvmStatic
        fun fromResource(source: OfferResource): OfferRecord {
            val createdAt = ApiDateUtil.tryParseDate(source.createdAt)
            return OfferRecord(
                    id = source.id.toLong(),
                    baseAmount = source.baseAmount,
                    baseAssetCode = source.baseAsset.id,
                    quoteAmount = source.quoteAmount,
                    quoteAssetCode = source.quoteAsset.id,
                    isBuy = source.isBuy,
                    date = createdAt,
                    fee = source.fee.calculatedPercent,
                    orderBookId = source.orderBookId.toLong(),
                    price = source.price,
                    baseBalanceId = source.baseBalance.id,
                    quoteBalanceId = source.quoteBalance.id
            )
        }

        @JvmStatic
        fun fromResource(source: OrderBookEntryResource): OfferRecord {
            return OfferRecord(
                    baseAmount = source.baseAmount,
                    baseAssetCode = source.baseAsset.id,
                    quoteAmount = source.quoteAmount,
                    quoteAssetCode = source.quoteAsset.id,
                    isBuy = source.isBuy,
                    date = source.createdAt,
                    price = source.price
            )
        }

        /**
         * @param source [BalanceChange] with [BalanceChangeCause.Offer] details
         */
        @JvmStatic
        fun fromBalanceChange(source: BalanceChange): OfferRecord {
            val details = source.cause as? BalanceChangeCause.Offer
                    ?: throw IllegalArgumentException("BalanceChangeCause.Offer is required")

            val baseBalanceId =
                    if (details.isBuy)
                        EMPTY_BALANCE_ID
                    else
                        source.balanceId

            val quoteBalanceId =
                    if (details.isBuy)
                        source.balanceId
                    else
                        EMPTY_BALANCE_ID

            return OfferRecord(
                    id = details.offerId,
                    orderBookId = details.orderBookId,
                    baseAmount = details.baseAmount,
                    baseAssetCode = details.baseAssetCode,
                    quoteAmount = details.quoteAmount,
                    quoteAssetCode = details.quoteAssetCode,
                    isBuy = details.isBuy,
                    date = source.date,
                    fee = details.fee.total,
                    price = details.price,
                    baseBalanceId = baseBalanceId,
                    quoteBalanceId = quoteBalanceId
            )
        }
    }
}