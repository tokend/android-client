package org.tokend.template.base.tfa

import android.content.Context
import android.text.InputType
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.sdk.federation.NeedTfaException
import org.tokend.template.R

class TfaPasswordDialog(context: Context, tfaVerifierInterface: TfaVerifier.Interface,
                        private val tfaException: NeedTfaException,
                        private val email: String)
    : TfaDialog(context, tfaVerifierInterface) {
    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            floatingLabelText = context.getString(R.string.password)
            hint = context.getString(R.string.password)
        }
    }

    override fun getOtp(input: CharArray): String {
        return PasswordTfaOtpGenerator().generate(tfaException, email, input)
    }

    override fun getMessage(): String {
        return "Enter your password"
    }

    override fun getInvalidInputError(): String {
        return context.getString(R.string.error_invalid_password)
    }
}