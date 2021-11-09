package io.tokend.template.features.history.model

import io.tokend.template.data.storage.repository.pagination.advanced.CursorPagingRecord
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.history.model.details.BalanceChangeCause
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
    val cause: BalanceChangeCause,
) : CursorPagingRecord, Serializable {
    override fun equals(other: Any?): Boolean {
        return other is BalanceChange && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override val pagingCursor: Long
        get() = id

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