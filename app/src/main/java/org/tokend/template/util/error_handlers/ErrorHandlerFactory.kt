package org.tokend.template.util.error_handlers

object ErrorHandlerFactory {
    private val defaultErrorHandler: ErrorHandler by lazy {
        DefaultErrorHandler()
    }

    fun getDefault(): ErrorHandler {
        return defaultErrorHandler
    }
}