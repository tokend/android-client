package io.tokend.template.util.errorhandler

/**
 * [ErrorHandler] that delegates handling to
 * inner handlers.
 *
 * @param innerHandlers handlers ordered by priority
 */
class CompositeErrorHandler(
    vararg innerHandlers: ErrorHandler
) : ErrorHandler {
    private val innerHandlers = innerHandlers.asList()
    private var successfulHandleCallback: (() -> Unit)? = null

    override fun handle(error: Throwable): Boolean {
        for (handler in innerHandlers) {
            if (handler.handle(error)) {
                successfulHandleCallback?.invoke()
                return true
            }
        }

        return false
    }

    override fun getErrorMessage(error: Throwable): String? {
        for (handler in innerHandlers) {
            val message = handler.getErrorMessage(error)
            if (message != null) {
                return message
            }
        }

        return null
    }

    fun doOnSuccessfulHandle(action: () -> Unit) =
        apply { successfulHandleCallback = action }
}