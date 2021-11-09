package io.tokend.template.util.errorhandler

interface ErrorLogger {
    fun log(error: Throwable)
}