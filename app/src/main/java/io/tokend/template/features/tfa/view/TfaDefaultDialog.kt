package io.tokend.template.features.tfa.view

import android.content.Context
import android.text.InputType
import io.tokend.template.R
import io.tokend.template.util.errorhandler.ErrorHandler
import org.tokend.sdk.tfa.TfaVerifier

/**
 * TFA dialog without specific code format.
 */
class TfaDefaultDialog(
    context: Context,
    errorHandler: ErrorHandler,
    tfaVerifierInterface: TfaVerifier.Interface
) : TfaOtpDialog(context, errorHandler, tfaVerifierInterface) {

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