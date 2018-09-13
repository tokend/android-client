package org.tokend.template.base.tfa

import android.content.Context
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.util.error_handlers.ErrorHandler

class TfaTotpDialog(context: Context,
                    errorHandler: ErrorHandler,
                    tfaVerifierInterface: TfaVerifier.Interface)
    : TfaOtpDialog(context, errorHandler, tfaVerifierInterface) {

    override fun getMessage(): String {
        return context.getString(R.string.totp_dialog_message)
    }

    override fun getMaxCodeLength(): Int = 6
}