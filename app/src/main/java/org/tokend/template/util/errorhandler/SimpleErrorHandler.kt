package org.tokend.template.util.errorhandler

class SimpleErrorHandler(
        private val handler: (Throwable) -> Boolean
): ErrorHandler {
    override fun handle(error: Throwable): Boolean = handler.invoke(error)

    override fun getErrorMessage(error: Throwable): String? = null
}