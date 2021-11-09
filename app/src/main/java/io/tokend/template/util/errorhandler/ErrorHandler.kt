package io.tokend.template.util.errorhandler

interface ErrorHandler {
    fun handle(error: Throwable): Boolean
    fun getErrorMessage(error: Throwable): String?

    fun handleIfPossible(error: Throwable) {
        handle(error)
    }
}