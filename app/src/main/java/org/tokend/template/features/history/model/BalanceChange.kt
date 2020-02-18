package org.tokend.template.features.history.model

import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.history.model.details.BalanceChangeCause
import org.tokend.template.data.repository.base.pagination.PagingRecord
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

open class BalanceChange(
        val id: Long,
        val action: BalanceChangeAction,
        val amount: BigDecimal,
        val asset: Asset,
        val balanceId: String,
        val fee: SimpleFeeRecord,
        val date: Date,
        val cause: BalanceChangeCause
) : PagingRecord, Serializable {
    override fun equals(other: Any?): Boolean {
        return other is BalanceChange && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun getPagingId(): Long = id

    val assetCode: String = asset.code

    val isReceived: Boolean? = when (action) {
        BalanceChangeAction.LOCKED -> false
        BalanceChangeAction.CHARGED_FROM_LOCKED -> false
        BalanceChangeAction.UNLOCKED -> true
        BalanceChangeAction.CHARGED -> false
        BalanceChangeAction.WITHDRAWN -> false
        BalanceChangeAction.MATCHED ->
            (cause as? BalanceChangeCause.MatchedOffer)
                    ?.isReceivedByAsset(assetCode)
        BalanceChangeAction.ISSUED -> true
        BalanceChangeAction.FUNDED -> true
    }

    /**
     * Amount including fee
     */
    val totalAmount =
            if (isReceived == true && action != BalanceChangeAction.UNLOCKED)
                amount - fee.total
            else
                amount + fee.total
}