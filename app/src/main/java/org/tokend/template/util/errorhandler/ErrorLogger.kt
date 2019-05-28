package org.tokend.template.util.errorhandler

interface ErrorLogger {
    fun log(error: Throwable)
}