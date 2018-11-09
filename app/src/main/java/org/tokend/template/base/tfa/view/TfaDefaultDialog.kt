package org.tokend.template.base.tfa.view

import android.content.Context
import android.text.InputType
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.util.error_handlers.ErrorHandler

class TfaDefaultDialog(context: Context,
                       errorHandler: ErrorHandler,
                       tfaVerifierInterface: TfaVerifier.Interface)
    : TfaOtpDialog(context, errorHandler, tfaVerifierInterface) {

    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            filters = arrayOf()
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    override fun getMessage(): String {
        return context.getString(R.string.tfa_default_dialog_message)
    }

    override fun getMaxCodeLength(): Int = -1
}