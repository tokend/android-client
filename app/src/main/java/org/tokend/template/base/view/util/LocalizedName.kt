package org.tokend.template.base.view.util

import android.content.Context
import org.tokend.sdk.api.models.transactions.Transaction
import org.tokend.sdk.api.models.transactions.Transaction.PaymentState.*
import org.tokend.template.R

class LocalizedName(private val context: Context) {
    fun forPaymentState(state: Transaction.PaymentState): String {
        return when (state) {
            PENDING -> context.getString(R.string.tx_state_pending)
            SUCCESS -> context.getString(R.string.tx_state_success)
            REJECTED -> context.getString(R.string.tx_state_rejected)
            CANCELED -> context.getString(R.string.tx_state_cancelled)
        }
    }
}