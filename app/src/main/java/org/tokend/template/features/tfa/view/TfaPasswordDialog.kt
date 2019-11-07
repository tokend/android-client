package org.tokend.template.features.tfa.view

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.InputType
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.logic.credentials.persistence.CredentialsPersistor
import org.tokend.template.logic.fingerprint.FingerprintAuthManager
import org.tokend.template.util.errorhandler.ErrorHandler
import org.tokend.template.view.FingerprintIndicatorManager
import org.tokend.template.view.ToastManager

/**
 * TFA verification dialog requesting user's password and
 * forming OTP from it.
 */
class TfaPasswordDialog(context: Context,
                        errorHandler: ErrorHandler,
                        tfaVerifierInterface: TfaVerifier.Interface,
                        credentialsPersistor: CredentialsPersistor?,
                        private val tfaException: NeedTfaException,
                        private val email: String,
                        private val toastManager: ToastManager?)
    : TfaDialog(context, errorHandler, tfaVerifierInterface) {
    private val fingerprintAuthManager = credentialsPersistor?.let {
        FingerprintAuthManager(context, it)
    }

    private lateinit var fingerprintIndicatorManager: FingerprintIndicatorManager

    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            floatingLabelText = context.getString(R.string.password)
            hint = context.getString(R.string.password)
        }

        inputButtonImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_fingerprint))
        fingerprintIndicatorManager =
                FingerprintIndicatorManager(context, inputButtonImageView, toastManager)
    }

    override fun afterDialogShown() {
        super.afterDialogShown()

        fingerprintIndicatorManager.hide()
        fingerprintAuthManager?.requestAuthIfAvailable(
                onAuthStart = {
                    fingerprintIndicatorManager.show()
                },
                onError = {
                    toastManager?.short(it)
                    fingerprintIndicatorManager.error()
                },
                onSuccess = { _, password ->
                    inputEditText.setText(password, 0, password.size)
                    inputEditText.setSelection(password.size)
                    password.fill('0')
                    tryToVerify()
                }
        )
    }

    override fun extendDialogBuilder(builder: AlertDialog.Builder) {
        super.extendDialogBuilder(builder)
        builder.setOnDismissListener {
            fingerprintAuthManager?.cancelAuth()
        }
    }

    override fun getOtp(input: CharArray): String {
        return PasswordTfaOtpGenerator().generate(tfaException, email, input)
    }

    override fun getMessage(): String {
        return context.getString(R.string.enter_your_password)
    }

    override fun getInvalidInputError(): String {
        return context.getString(R.string.error_invalid_password)
    }
}