package io.tokend.template.features.tfa.view

import android.content.Context
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.tokend.template.R
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.util.biometric.BiometricAuthManager
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.view.ToastManager
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaVerifier

/**
 * TFA verification dialog requesting user's password and
 * forming OTP from it.
 */
class TfaPasswordDialog(
    context: Context,
    errorHandler: ErrorHandler,
    tfaVerifierInterface: TfaVerifier.Interface,
    credentialsPersistence: CredentialsPersistence?,
    private val tfaException: NeedTfaException,
    private val login: String,
    private val toastManager: ToastManager?,
) : TfaDialog(context, errorHandler, tfaVerifierInterface) {
    private val biometricAuthManager = credentialsPersistence?.let {
        BiometricAuthManager(context as FragmentActivity, it)
    }

    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            floatingLabelText = context.getString(R.string.password)
            hint = context.getString(R.string.password)
        }

        if (biometricAuthManager?.isAuthPossible == true) {
            inputButtonImageView.visibility = View.VISIBLE
            inputButtonImageView.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_fingerprint
                )
            )
            inputButtonImageView.setOnClickListener {
                requestAuthIfPossible()
            }
        }
    }

    override fun afterDialogShown() {
        super.afterDialogShown()
        requestAuthIfPossible()
    }

    private fun requestAuthIfPossible() {
        biometricAuthManager?.requestAuthIfPossible(
            onError = {
                toastManager?.short(it?.toString())
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
            biometricAuthManager?.cancelAuth()
        }
    }

    override fun getOtp(input: CharArray): String {
        return PasswordTfaOtpGenerator().generate(tfaException, login, input)
    }

    override fun getMessage(): String {
        return context.getString(R.string.enter_your_password)
    }

    override fun getInvalidInputError(): String {
        return context.getString(R.string.error_invalid_password)
    }
}