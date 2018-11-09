package org.tokend.template.base.tfa.view

import android.content.Context
import android.support.annotation.StyleRes
import android.support.v7.app.AlertDialog
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.jetbrains.anko.browse
import org.jetbrains.anko.clipboardManager
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.R
import org.tokend.template.util.ToastManager

/**
 * This dialog provides TOTP seed display
 * and ability to copy it or open totp:// URI
 * in order to add the seed to an authenticator app.
 */
class TotpFactorConfirmationDialog(
        private val context: Context,
        private val toastManager: ToastManager?,
        @StyleRes
        private val style: Int? = null
) {
    /**
     * Displays dialog.
     * Result [Single] will emmit true if user confirmed adding
     * the seed the authenticator, false otherwise.
     */
    fun show(factor: TfaFactor): Single<Boolean> {
        val resultSubject = SingleSubject.create<Boolean>()

        val secret = factor.attributes.secret
        val seed = factor.attributes.seed
                ?: return Single.error(IllegalStateException("Factor seed is required"))

        val builder = if (style != null)
            AlertDialog.Builder(context, style)
        else
            AlertDialog.Builder(context)

        val dialog = builder
                .setTitle(R.string.tfa_add_dialog_title)
                .setMessage(context.getString(R.string.template_tfa_add_dialog_message, secret))
                .setOnCancelListener {
                    resultSubject.onSuccess(false)
                }
                .setPositiveButton(R.string.continue_action) { _, _ ->
                    resultSubject.onSuccess(true)
                }
                .setNegativeButton(R.string.open_action, null)
                .setNeutralButton(R.string.copy_action, null)
                .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            context.clipboardManager.text = secret
            toastManager?.short(R.string.tfa_key_copied)
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            context.browse(seed)
        }

        return resultSubject
    }
}