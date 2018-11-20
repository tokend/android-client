package org.tokend.template.util.errorhandler

import android.content.Context
import org.tokend.sdk.api.transactions.model.TransactionFailedException
import org.tokend.template.R
import org.tokend.template.view.ToastManager
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

open class DefaultErrorHandler(
        private val context: Context
) : ErrorHandler {
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
                    ToastManager(context).short(it)
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
            is SocketTimeoutException ->
                context.getString(R.string.error_connection_try_again)
            is CancellationException, is InterruptedIOException ->
                null
            is IOException ->
                context.getString(R.string.error_connection_try_again)
            is TransactionFailedException ->
                getTransactionFailedMessage(error)
            else -> {
                context.getString(R.string.error_try_again)
            }
        }
    }

    private fun getTransactionFailedMessage(error: TransactionFailedException): String? {
        return when (error.transactionResultCode) {
            TransactionFailedException.TX_FAILED ->
                when (error.firstOperationResultCode) {
                    TransactionFailedException.OP_LIMITS_EXCEEDED ->
                        context.getString(R.string.error_tx_limits)
                    TransactionFailedException.OP_INSUFFICIENT_BALANCE ->
                        context.getString(R.string.error_tx_insufficient_balance)
                    TransactionFailedException.OP_INVALID_AMOUNT ->
                        context.getString(R.string.error_tx_invalid_amount)
                    TransactionFailedException.OP_MALFORMED ->
                        context.getString(R.string.error_tx_malformed)
                    TransactionFailedException.OP_ACCOUNT_BLOCKED ->
                        context.getString(R.string.error_tx_account_blocked)
                    TransactionFailedException.OP_INVALID_FEE ->
                        context.getString(R.string.error_tx_invalid_fee)
                    TransactionFailedException.OP_NOT_ALLOWED ->
                        context.getString(R.string.error_tx_not_allowed)
                    TransactionFailedException.OP_OFFER_CROSS_SELF ->
                        context.getString(R.string.error_tx_cross_self)
                    TransactionFailedException.OP_AMOUNT_LESS_THEN_DEST_FEE ->
                        context.getString(R.string.error_payment_amount_less_than_fee)
                    TransactionFailedException.OP_REQUIRES_KYC ->
                        context.getString(R.string.error_kyc_required_to_own_asset)
                    else ->
                        context.getString(R.string.error_tx_general)
                }
            TransactionFailedException.TX_BAD_AUTH ->
                context.getString(R.string.error_tx_bad_auth)
            else ->
                context.getString(R.string.error_tx_general)
        }
    }
}