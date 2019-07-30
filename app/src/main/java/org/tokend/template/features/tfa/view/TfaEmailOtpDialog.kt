package org.tokend.template.features.tfa.view

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.util.errorhandler.ErrorHandler

/**
 * TFA verification dialog requesting code from the email.
 */
class TfaEmailOtpDialog(context: Context,
                        errorHandler: ErrorHandler,
                        tfaVerifierInterface: TfaVerifier.Interface)
    : TfaOtpDialog(context, errorHandler, tfaVerifierInterface) {

    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            filters = arrayOf()
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            floatingLabelText = context.getString(R.string.otp_verification_code)
            hint = context.getString(R.string.otp_verification_code)
        }
    }

    override fun getMessage(): String {
        return context.getString(R.string.email_otp_dialog_message)
    }

    override fun getMaxCodeLength(): Int = -1
}