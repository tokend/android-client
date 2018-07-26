package org.tokend.template.base.view.util

import android.content.Context
import org.tokend.sdk.api.models.transactions.TransactionState
import org.tokend.template.R
import org.tokend.template.base.view.adapter.history.TxHistoryItem

class LocalizedName(private val context: Context) {
    fun forTransactionState(state: TransactionState): String {
        return when (state) {
            TransactionState.PENDING -> context.getString(R.string.tx_state_pending)
            TransactionState.SUCCESS -> context.getString(R.string.tx_state_success)
            TransactionState.REJECTED -> context.getString(R.string.tx_state_rejected)
            TransactionState.CANCELED -> context.getString(R.string.tx_state_cancelled)
            TransactionState.FAILED -> context.getString(R.string.tx_state_failed)
        }
    }

    fun forTransactionAction(action: TxHistoryItem.Action): String {
        return when(action) {
            TxHistoryItem.Action.DEPOSIT -> context.getString(R.string.tx_action_deposit)
            TxHistoryItem.Action.WITHDRAWAL -> context.getString(R.string.tx_action_withdrawal)
            TxHistoryItem.Action.INVESTMENT -> context.getString(R.string.tx_action_investment)
            TxHistoryItem.Action.RECEIVED -> context.getString(R.string.tx_action_received)
            TxHistoryItem.Action.SENT -> context.getString(R.string.tx_action_sent)
            TxHistoryItem.Action.BOUGHT -> context.getString(R.string.tx_action_bought)
            TxHistoryItem.Action.SOLD -> context.getString(R.string.tx_action_sold)
            TxHistoryItem.Action.SPENT -> context.getString(R.string.tx_action_spent)
            TxHistoryItem.Action.BUY -> context.getString(R.string.buy)
            TxHistoryItem.Action.SELL -> context.getString(R.string.sell)
        }
    }
}