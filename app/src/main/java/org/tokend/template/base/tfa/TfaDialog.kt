package org.tokend.template.base.tfa

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.rengwuxian.materialedittext.MaterialEditText
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.uiThread
import org.tokend.sdk.api.tfa.InvalidOtpException
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

abstract class TfaDialog(protected val context: Context,
                         private val tfaVerifierInterface: TfaVerifier.Interface
) {
    protected val inputEditText: MaterialEditText
    protected val inputButtonImageView: AppCompatImageView
    protected val messageTextView: TextView
    protected lateinit var progress: ProgressBar
    protected val dialog: AlertDialog

    protected val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.visibility = View.VISIBLE },
            hideLoading = { progress.visibility = View.GONE }
    )

    init {
        val view = context.layoutInflater.inflate(R.layout.dialog_tfa, null)

        progress = view.find(R.id.progress)
        messageTextView = view.find(R.id.tfa_message_text_view)
        inputEditText = view.find(R.id.tfa_input_edit_text)
        inputButtonImageView = view.find(R.id.tfa_input_button_image_view)

        dialog = AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.tfa_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.continue_action, null)
                .setNegativeButton(R.string.cancel) { _, _ ->
                    tfaVerifierInterface.cancelVerification()
                }
                .setOnCancelListener {
                    tfaVerifierInterface.cancelVerification()
                }
                .also {
                    extendDialogBuilder(it)
                }
                .create()
    }

    open fun show() {
        if (!dialog.isShowing) {
            beforeDialogShow()
            dialog.show()
            afterDialogShown()
        }
    }

    protected open fun extendDialogBuilder(builder: AlertDialog.Builder) {}

    protected open fun beforeDialogShow() {
        inputEditText.onEditorAction {
            tryToVerify()
        }
        messageTextView.text = getMessage()
    }

    protected open fun afterDialogShown() {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    tryToVerify()
                }
    }

    protected open fun tryToVerify() {
        if (canVerify()) {
            verify()
        }
    }

    protected open fun canVerify(): Boolean {
        return !inputEditText.hasError() && !loadingIndicator.isLoading
    }

    protected open fun getOtp(input: CharArray): String {
        return input.joinToString("")
    }

    protected open fun verify() {
        loadingIndicator.show()

        doAsync {
            val inputLength = inputEditText.text.length
            val input = CharArray(inputLength)
            inputEditText.text.getChars(0, inputLength, input, 0)
            val otp = getOtp(input)

            uiThread {
                if (!dialog.isShowing) {
                    return@uiThread
                }

                tfaVerifierInterface.verify(otp,
                        onSuccess = {
                            loadingIndicator.hide()
                            dialog.dismiss()
                        },
                        onError = { error ->
                            loadingIndicator.hide()
                            if (error is InvalidOtpException) {
                                inputEditText.setErrorAndFocus(getInvalidInputError())
                            } else if (error != null) {
                                ErrorHandlerFactory.getDefault().handle(error)
                            }
                        })
            }
        }
    }

    abstract fun getMessage(): String
    abstract fun getInvalidInputError(): String
}