package org.tokend.template.base.tfa

import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputFilter
import android.text.InputType
import org.jetbrains.anko.clipboardManager
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.template.R

class TfaTotpDialog(context: Context, tfaVerifierInterface: TfaVerifier.Interface)
    : TfaDialog(context, tfaVerifierInterface) {

    override fun extendDialogBuilder(builder: AlertDialog.Builder) {
        builder.setNeutralButton(R.string.paste, null)
    }

    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            filters = arrayOf(InputFilter.LengthFilter(6))
            inputType = InputType.TYPE_CLASS_NUMBER
            floatingLabelText = context.getString(R.string.totp_verification_code)
            hint = context.getString(R.string.totp_verification_code)
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
        return context.getString(R.string.error_invalid_totp_code)
    }

    override fun getMessage(): String {
        return context.getString(R.string.totp_dialog_message)
    }
}