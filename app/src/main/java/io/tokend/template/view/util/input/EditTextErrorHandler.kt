package io.tokend.template.view.util.input

import android.widget.EditText
import io.tokend.template.extensions.setErrorAndFocus
import io.tokend.template.util.errorhandler.ErrorHandler

/**
 * Uses [editText] to display error messages.
 */
class EditTextErrorHandler(
    private val editText: EditText,
    private val messageProvider: (Throwable) -> String?
) : ErrorHandler {
    override fun handle(error: Throwable): Boolean {
        val message = getErrorMessage(error)
        return if (message != null) {
            editText.setErrorAndFocus(message)
            true
        } else {
            false
        }
    }

    override fun getErrorMessage(error: Throwable): String? = messageProvider.invoke(error)
}