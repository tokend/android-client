package org.tokend.template.features.tfa.view

import android.content.Context
import android.support.annotation.StyleRes
import android.support.v7.app.AlertDialog
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.jetbrains.anko.browse
import org.jetbrains.anko.clipboardManager
import org.tokend.template.R
import org.tokend.template.features.tfa.model.TotpTfaFactorRecord
import org.tokend.template.view.ToastManager

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
    fun show(factor: TotpTfaFactorRecord): Single<Boolean> {
        val resultSubject = SingleSubject.create<Boolean>()

        val secret = factor.secret
        val seed = factor.seed

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
            openAuthenticatorOrGooglePlay(seed)
        }

        return resultSubject
    }

    private fun openAuthenticatorOrGooglePlay(uri: String) {
        if (!context.browse(uri)) {
            context.browse(GOOGLE_PLAY_AUTHENTICATOR_URI)
        }
    }

    private companion object {
        private const val GOOGLE_PLAY_AUTHENTICATOR_URI =
                "https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2"
    }
}