package org.tokend.template.features.wallet.adapter

import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.data.model.history.details.BalanceChangeDetails
import org.tokend.template.util.DateProvider
import java.math.BigDecimal
import java.util.*

class BalanceChangeListItem(
        val action: Action,
        val amount: BigDecimal,
        val assetCode: String,
        val isReceived: Boolean,
        override val date: Date,
        val counterparty: String?,
        val source: BalanceChange? = null
) : DateProvider {
    enum class Action {
        LOCKED,
        UNLOCKED,
        WITHDRAWN,
        MATCHED,
        ISSUED,
        RECEIVED,
        SENT,
        CHARGED
    }

    constructor(balanceChange: BalanceChange,
                accountId: String) : this(
            action = getAction(balanceChange),
            amount = balanceChange.amount,
            assetCode = balanceChange.assetCode,
            isReceived = isReceived(balanceChange),
            date = balanceChange.date,
            counterparty = getCounterparty(balanceChange, accountId),
            source = balanceChange
    )

    private companion object {
        private fun getAction(balanceChange: BalanceChange): Action {
            return when (balanceChange.action) {
                BalanceChangeAction.LOCKED -> BalanceChangeListItem.Action.LOCKED
                BalanceChangeAction.CHARGED_FROM_LOCKED -> BalanceChangeListItem.Action.CHARGED
                BalanceChangeAction.CHARGED ->
                    if (balanceChange.details is BalanceChangeDetails.Payment)
                        BalanceChangeListItem.Action.SENT
                    else
                        BalanceChangeListItem.Action.CHARGED
                BalanceChangeAction.UNLOCKED -> BalanceChangeListItem.Action.UNLOCKED
                BalanceChangeAction.WITHDRAWN -> BalanceChangeListItem.Action.WITHDRAWN
                BalanceChangeAction.MATCHED -> BalanceChangeListItem.Action.MATCHED
                BalanceChangeAction.ISSUED -> BalanceChangeListItem.Action.ISSUED
                BalanceChangeAction.FUNDED -> BalanceChangeListItem.Action.RECEIVED
            }
        }

        private fun getCounterparty(balanceChange: BalanceChange, accountId: String): String? {
            val details = balanceChange.details

            if (balanceChange.action == BalanceChangeAction.LOCKED
                    || balanceChange.action == BalanceChangeAction.UNLOCKED) {
                // Do not show "Locked to ole21@mail.com"
                return null
            }

            return when (details) {
                is BalanceChangeDetails.Payment ->
                    details.getCounterpartyName(accountId)
                            ?: details.getCounterpartyAccountId(accountId)
                is BalanceChangeDetails.Withdrawal ->
                    details.destinationAddress
                else -> null
            }
        }

        private fun isReceived(balanceChange: BalanceChange): Boolean {
            return when (balanceChange.action) {
                BalanceChangeAction.LOCKED -> false
                BalanceChangeAction.CHARGED_FROM_LOCKED -> false
                BalanceChangeAction.UNLOCKED -> true
                BalanceChangeAction.CHARGED -> false
                BalanceChangeAction.WITHDRAWN -> false
                BalanceChangeAction.MATCHED ->
                    (balanceChange.details as BalanceChangeDetails.OfferMatch)
                            .isReceivedByAsset(balanceChange.assetCode)
                BalanceChangeAction.ISSUED -> true
                BalanceChangeAction.FUNDED -> true
            }
        }
    }
}