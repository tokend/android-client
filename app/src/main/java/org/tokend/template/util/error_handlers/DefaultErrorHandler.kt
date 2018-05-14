package org.tokend.template.util.error_handlers

import org.tokend.template.App
import org.tokend.template.R
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
            else -> {
                App.context.getString(R.string.error_try_again)
            }
        }
    }
}