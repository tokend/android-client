package org.tokend.template.util.errorhandler

import android.content.Context
import org.tokend.template.view.ToastManager

class ErrorHandlerFactory(
        private val context: Context,
        private val toastManager: ToastManager
) {
    private val defaultErrorHandler: ErrorHandler by lazy {
        DefaultErrorHandler(context, toastManager)
    }

    fun getDefault(): ErrorHandler {
        return defaultErrorHandler
    }
}