package org.tokend.template.features.wallet.adapter

import org.tokend.template.data.model.history.BalanceChange
import org.tokend.template.data.model.history.BalanceChangeAction
import org.tokend.template.data.model.history.details.OfferMatchDetails
import org.tokend.template.data.model.history.details.PaymentDetails
import org.tokend.template.data.model.history.details.WithdrawalDetails
import org.tokend.template.util.DateProvider
import java.math.BigDecimal
import java.util.*

class BalanceChangeListItem(
        val action: BalanceChangeAction,
        val amount: BigDecimal,
        val assetCode: String,
        val isReceived: Boolean,
        override val date: Date,
        val counterparty: String?,
        val source: BalanceChange? = null
): DateProvider {
    constructor(balanceChange: BalanceChange,
                accountId: String) : this(
            action = balanceChange.action,
            amount = balanceChange.amount,
            assetCode = balanceChange.assetCode,
            isReceived = isReceived(balanceChange),
            date = balanceChange.date,
            counterparty = getCounterparty(balanceChange, accountId),
            source = balanceChange
    )

    private companion object {
        private fun getCounterparty(balanceChange: BalanceChange, accountId: String): String? {
            val details = balanceChange.details

            return when (details) {
                is PaymentDetails ->
                    details.getCounterpartyName(accountId)
                            ?: details.getCounterpartyAccountId(accountId)
                is WithdrawalDetails ->
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
                    (balanceChange.details as OfferMatchDetails)
                            .isReceivedByAsset(balanceChange.assetCode)
                BalanceChangeAction.ISSUED -> true
                BalanceChangeAction.FUNDED -> true
            }
        }
    }
}