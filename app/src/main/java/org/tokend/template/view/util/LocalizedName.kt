package org.tokend.template.view.util

import android.content.Context
import org.tokend.sdk.api.base.model.operations.OperationState
import org.tokend.template.R
import org.tokend.template.view.adapter.history.TxHistoryItem

class LocalizedName(private val context: Context) {
    fun forTransactionState(state: OperationState): String {
        return when (state) {
            OperationState.PENDING -> context.getString(R.string.tx_state_pending)
            OperationState.SUCCESS -> context.getString(R.string.tx_state_success)
            OperationState.REJECTED -> context.getString(R.string.tx_state_rejected)
            OperationState.CANCELED -> context.getString(R.string.tx_state_cancelled)
            OperationState.FAILED -> context.getString(R.string.tx_state_failed)
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