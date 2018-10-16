package org.tokend.template.base.view.adapter.history

import org.tokend.sdk.api.base.model.transactions.*
import java.math.BigDecimal
import java.util.*

class TxHistoryItem(
        val amount: BigDecimal,
        val asset: String,
        val action: Action,
        val counterparty: String?,
        val state: TransactionState,
        val date: Date,
        val isReceived: Boolean,
        val source: Transaction? = null
) {
    enum class Action {
        DEPOSIT,
        WITHDRAWAL,
        INVESTMENT,
        RECEIVED,
        SENT,
        BOUGHT,
        SOLD,
        SPENT,
        BUY,
        SELL;
    }

    companion object {
        fun fromTransaction(tx: Transaction): TxHistoryItem {
            val action =
                    when (tx.type) {
                        TransactionType.ISSUANCE -> Action.DEPOSIT
                        TransactionType.WITHDRAWAL -> Action.WITHDRAWAL
                        TransactionType.INVESTMENT -> Action.INVESTMENT
                        TransactionType.OFFER_MATCH ->
                            if (tx.state == TransactionState.PENDING) {
                                if (tx.isReceived)
                                    Action.BUY
                                else
                                    Action.SELL
                            } else {
                                if (tx.isReceived)
                                    Action.BOUGHT
                                else
                                    Action.SOLD
                            }
                        TransactionType.PAYMENT ->
                            if (tx.isReceived)
                                Action.RECEIVED
                            else
                                Action.SENT
                        else ->
                            if (tx.isReceived)
                                Action.RECEIVED
                            else
                                Action.SPENT

                    }

            var counterparty: String? = null
            var amount = tx.amount
            var asset = tx.asset
            var isReceived = tx.isReceived

            if (tx is PaymentTransaction) {
                counterparty =
                        if (tx.isReceived)
                            tx.counterpartyNickname ?: tx.sourceAccount
                        else
                            tx.counterpartyNickname ?: tx.destAccount
            } else if (tx is MatchTransaction
                    && !(action == TxHistoryItem.Action.INVESTMENT && tx.isReceived)) {
                counterparty = tx.matchData.quoteAsset
            } else if (tx is WithdrawalTransaction) {
                counterparty = tx.destAddress
            }else if(tx is InvestmentTransaction){
                if (tx.state == TransactionState.PENDING) {
                    isReceived = false
                }

                if (!isReceived) {
                    counterparty = tx.asset
                    asset = tx.matchData.quoteAsset
                    amount = tx.matchData.quoteAmount
                }
            }

            return TxHistoryItem(amount, asset, action, counterparty, tx.state, tx.date,
                    isReceived, tx)
        }
    }
}