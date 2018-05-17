package org.tokend.template.util.error_handlers

import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.base.logic.transactions.TransactionFailedException
import org.tokend.template.util.ToastManager
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.CancellationException

open class DefaultErrorHandler : ErrorHandler {
    /**
     * Handles given [Throwable]
     * @return [true] if [error] was handled, [false] otherwise
     */
    override fun handle(error: Throwable): Boolean {
        when (error) {
            is CancellationException ->
                return true
            else -> {
                return getErrorMessage(error)?.let {
                    ToastManager.short(it)
                    true
                } ?: false
            }
        }
    }

    /**
     * @return Localized error message for given [Throwable]
     */
    override fun getErrorMessage(error: Throwable): String? {
        return when (error) {
            is CancellationException, is InterruptedIOException ->
                null
            is IOException ->
                App.context.getString(R.string.error_connection_try_again)
            is TransactionFailedException ->
                getTransactionFailedMessage(error)
            else -> {
                App.context.getString(R.string.error_try_again)
            }
        }
    }

    private fun getTransactionFailedMessage(error: TransactionFailedException): String? {
        return when (error.transactionResultCode) {
            TransactionFailedException.TX_FAILED ->
                when (error.firstOperationResultCode) {
                    TransactionFailedException.OP_LIMITS_EXCEEDED ->
                        App.context.getString(R.string.error_tx_limits)
                    TransactionFailedException.OP_INSUFFICIENT_BALANCE ->
                        App.context.getString(R.string.error_tx_insufficient_balance)
                    TransactionFailedException.OP_INVALID_AMOUNT ->
                        App.context.getString(R.string.error_tx_invalid_amount)
                    TransactionFailedException.OP_MALFORMED ->
                        App.context.getString(R.string.error_tx_malformed)
                    TransactionFailedException.OP_ACCOUNT_BLOCKED ->
                        App.context.getString(R.string.error_tx_account_blocked)
                    TransactionFailedException.OP_INVALID_FEE ->
                        App.context.getString(R.string.error_tx_invalid_fee)
                    TransactionFailedException.OP_NOT_ALLOWED ->
                        App.context.getString(R.string.error_tx_not_allowed)
                    TransactionFailedException.OP_OFFER_CROSS_SELF ->
                        App.context.getString(R.string.error_tx_cross_self)
                    else ->
                        App.context.getString(R.string.error_tx_general)
                }
            TransactionFailedException.TX_BAD_AUTH ->
                App.context.getString(R.string.error_tx_bad_auth)
            else ->
                App.context.getString(R.string.error_tx_general)
        }
    }
}