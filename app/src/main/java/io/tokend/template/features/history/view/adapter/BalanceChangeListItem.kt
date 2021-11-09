package io.tokend.template.features.history.view.adapter

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.history.model.BalanceChange
import io.tokend.template.features.history.model.BalanceChangeAction
import io.tokend.template.features.history.model.details.BalanceChangeCause
import io.tokend.template.util.DateProvider
import io.tokend.template.view.util.LocalizedName
import java.math.BigDecimal
import java.util.*

class BalanceChangeListItem(
    val action: Action,
    val amount: BigDecimal,
    val asset: Asset,
    val isReceived: Boolean?,
    override val date: Date,
    val counterparty: String?,
    val causeName: String?,
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

    constructor(
        balanceChange: BalanceChange,
        accountId: String,
        localizedName: LocalizedName
    ) : this(
        action = getAction(balanceChange),
        amount = balanceChange.totalAmount,
        asset = balanceChange.asset,
        isReceived = balanceChange.isReceived,
        date = balanceChange.date,
        counterparty = getCounterparty(balanceChange, accountId),
        causeName = getCauseName(balanceChange.cause, localizedName),
        source = balanceChange
    )

    private companion object {
        private fun getAction(balanceChange: BalanceChange): Action {
            return when (balanceChange.action) {
                BalanceChangeAction.LOCKED -> Action.LOCKED
                BalanceChangeAction.CHARGED_FROM_LOCKED -> Action.CHARGED
                BalanceChangeAction.CHARGED ->
                    if (balanceChange.cause is BalanceChangeCause.Payment)
                        Action.SENT
                    else
                        Action.CHARGED
                BalanceChangeAction.UNLOCKED -> Action.UNLOCKED
                BalanceChangeAction.WITHDRAWN -> Action.WITHDRAWN
                BalanceChangeAction.MATCHED -> Action.MATCHED
                BalanceChangeAction.ISSUED -> Action.ISSUED
                BalanceChangeAction.FUNDED -> Action.RECEIVED
            }
        }

        private fun getCounterparty(balanceChange: BalanceChange, accountId: String): String? {
            val details = balanceChange.cause

            if (balanceChange.action == BalanceChangeAction.LOCKED
                || balanceChange.action == BalanceChangeAction.UNLOCKED
            ) {
                // Do not show "Locked to ole21@mail.com"
                return null
            }

            return when (details) {
                is BalanceChangeCause.Payment ->
                    details.getCounterpartyName(accountId)
                        ?: formatAccountId(details.getCounterpartyAccountId(accountId))
                is BalanceChangeCause.WithdrawalRequest ->
                    details.destinationAddress
                else -> null
            }
        }

        private fun formatAccountId(accountId: String): String {
            return "${accountId.substring(0..3)}…" +
                    accountId.substring(accountId.length - 4 until accountId.length)
        }

        private fun getCauseName(cause: BalanceChangeCause, localizedName: LocalizedName): String? {
            return if (cause !is BalanceChangeCause.Unknown)
                localizedName.forBalanceChangeCause(cause)
            else
                null
        }
    }
}