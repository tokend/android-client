package io.tokend.template.features.tfa.view

import android.content.Context
import io.tokend.template.R
import io.tokend.template.util.errorhandler.ErrorHandler
import org.tokend.sdk.tfa.TfaVerifier

/**
 * TFA verification dialog requesting code from the TOTP authenticator app.
 */
class TfaTotpDialog(
    context: Context,
    errorHandler: ErrorHandler,
    tfaVerifierInterface: TfaVerifier.Interface
) : TfaOtpDialog(context, errorHandler, tfaVerifierInterface) {

    override fun getMessage(): String {
        return context.getString(R.string.totp_dialog_message)
    }

    override fun getMaxCodeLength(): Int = 6
}