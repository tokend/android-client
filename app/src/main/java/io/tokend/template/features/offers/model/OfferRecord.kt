package io.tokend.template.features.offers.model

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.history.model.BalanceChange
import io.tokend.template.features.history.model.details.BalanceChangeCause
import org.tokend.sdk.api.v3.model.generated.resources.OfferResource
import org.tokend.wallet.Base32Check
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class OfferRecord(
    val baseAsset: Asset,
    val quoteAsset: Asset,
    val price: BigDecimal,
    val isBuy: Boolean,
    val baseAmount: BigDecimal,
    val quoteAmount: BigDecimal = baseAmount * price,
    val id: Long = 0L,
    val orderBookId: Long = 0L,
    val date: Date = Date(),
    var baseBalanceId: String = EMPTY_BALANCE_ID,
    var quoteBalanceId: String = EMPTY_BALANCE_ID,
    val fee: BigDecimal = BigDecimal.ZERO
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
            return OfferRecord(
                id = source.id.toLong(),
                baseAmount = source.baseAmount,
                baseAsset = SimpleAsset(source.baseAsset),
                quoteAmount = source.quoteAmount,
                quoteAsset = SimpleAsset(source.quoteAsset),
                isBuy = source.isBuy,
                date = source.createdAt,
                fee = source.fee.calculatedPercent,
                orderBookId = source.orderBookId.toLong(),
                price = source.price,
                baseBalanceId = source.baseBalance.id,
                quoteBalanceId = source.quoteBalance.id
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
                id = details.offerId ?: 0,
                orderBookId = details.orderBookId,
                baseAmount = details.baseAmount,
                baseAsset = SimpleAsset(details.baseAssetCode),
                quoteAmount = details.quoteAmount,
                quoteAsset = SimpleAsset(details.quoteAssetCode),
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