package org.tokend.template.view.adapter.history

import org.tokend.sdk.api.base.model.operations.*
import java.math.BigDecimal
import java.util.*

class TxHistoryItem(
        val amount: BigDecimal,
        val asset: String,
        val action: Action,
        val counterparty: String?,
        val state: OperationState,
        val date: Date,
        val isReceived: Boolean,
        val source: TransferOperation? = null
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
        fun fromTransaction(tx: TransferOperation): TxHistoryItem {
            val action =
                    when (tx.type) {
                        OperationType.ISSUANCE -> Action.DEPOSIT
                        OperationType.WITHDRAWAL -> Action.WITHDRAWAL
                        OperationType.INVESTMENT -> Action.INVESTMENT
                        OperationType.OFFER_MATCH ->
                            if (tx.state == OperationState.PENDING) {
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
                        OperationType.PAYMENT ->
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

            if (tx is PaymentOperation) {
                counterparty =
                        if (tx.isReceived)
                            tx.counterpartyNickname ?: tx.sourceAccount
                        else
                            tx.counterpartyNickname ?: tx.destAccount
            } else if (tx is OfferMatchOperation
                    && !(action == TxHistoryItem.Action.INVESTMENT && tx.isReceived)) {
                counterparty = tx.matchData.quoteAsset
            } else if (tx is WithdrawalOperation) {
                counterparty = tx.destAddress
            }else if(tx is InvestmentOperation){
                if (tx.state == OperationState.PENDING) {
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