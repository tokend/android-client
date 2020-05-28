package org.tokend.template.features.tfa.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import com.rengwuxian.materialedittext.MaterialEditText
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.tokend.sdk.tfa.InvalidOtpException
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.errorhandler.ErrorHandler
import org.tokend.template.view.util.LoadingIndicatorManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Abstract TFA verification dialog with code input.
 */
abstract class TfaDialog(protected val context: Context,
                         protected val errorHandler: ErrorHandler,
                         private val tfaVerifierInterface: TfaVerifier.Interface
) {
    protected val inputEditText: MaterialEditText
    protected val inputButtonImageView: AppCompatImageView
    protected val messageTextView: TextView
    protected lateinit var progress: ProgressBar
    protected val dialog: AlertDialog

    protected val verificationExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    protected val mainThreadHandler: Handler = Handler(Looper.getMainLooper())

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
                .setOnDismissListener {
                    verificationExecutor.shutdownNow()
                }
                .also {
                    extendDialogBuilder(it)
                }
                .create()
    }

    open fun show() {
        if (!dialog.isShowing) {
            beforeDialogShow()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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

        val input = inputEditText.text.getChars()

        verificationExecutor.submit {
            tfaVerifierInterface.verify(
                    otp = getOtp(input),
                    onSuccess = {
                        mainThreadHandler.post(this::onSuccessfulVerification)
                    },
                    onError = { error ->
                        mainThreadHandler.post { onVerificationError(error) }
                    }
            )
        }
    }

    protected open fun onSuccessfulVerification() {
        if (!dialog.isShowing) {
            return
        }

        loadingIndicator.hide()
        dialog.dismiss()
    }

    protected open fun onVerificationError(error: Throwable?) {
        if (!dialog.isShowing) {
            return
        }

        loadingIndicator.hide()
        if (error is InvalidOtpException) {
            inputEditText.setErrorAndFocus(getInvalidInputError())
        } else if (error != null) {
            errorHandler.handle(error)
        }
    }

    abstract fun getMessage(): String
    abstract fun getInvalidInputError(): String
}