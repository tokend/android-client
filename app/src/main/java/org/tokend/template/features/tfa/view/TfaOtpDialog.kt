package org.tokend.template.features.tfa.view

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.text.InputFilter
import android.text.InputType
import org.jetbrains.anko.clipboardManager
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.util.errorhandler.ErrorHandler

/**
 * Abstract TFA verification dialog for plain one-time password.
 */
abstract class TfaOtpDialog(context: Context,
                            errorHandler: ErrorHandler,
                            tfaVerifierInterface: TfaVerifier.Interface)
    : TfaDialog(context, errorHandler, tfaVerifierInterface) {

    override fun extendDialogBuilder(builder: AlertDialog.Builder) {
        builder.setNeutralButton(R.string.paste, null)
    }

    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            filters = arrayOf(InputFilter.LengthFilter(getMaxCodeLength()))
            inputType = InputType.TYPE_CLASS_NUMBER
            floatingLabelText = context.getString(R.string.otp_verification_code)
            hint = context.getString(R.string.otp_verification_code)
        }
    }

    override fun afterDialogShown() {
        super.afterDialogShown()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener {
                    inputEditText.setText(context.clipboardManager.text)
                    inputEditText.requestFocus()
                    inputEditText.setSelection(inputEditText.text?.length ?: 0)
                }
    }

    override fun getInvalidInputError(): String {
        return context.getString(R.string.error_invalid_otp_code)
    }

    abstract fun getMaxCodeLength(): Int
}