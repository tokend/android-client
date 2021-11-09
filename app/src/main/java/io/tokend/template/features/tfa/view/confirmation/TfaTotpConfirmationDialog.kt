package io.tokend.template.features.tfa.view.confirmation

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import io.tokend.template.R
import io.tokend.template.extensions.browse
import io.tokend.template.extensions.clipboardText
import io.tokend.template.view.ToastManager

/**
 * This dialog provides TOTP seed display
 * and ability to copy it or open totp:// URI
 * in order to add the seed to an authenticator app.
 */
class TfaTotpConfirmationDialog(
    context: Context,
    confirmationAttributes: Map<String, Any>,
    private val toastManager: ToastManager?,
    @StyleRes
    style: Int? = null
) : TfaConfirmationDialog(context, confirmationAttributes, style) {
    override fun show(confirmationCallback: (Boolean) -> Unit) {
        val secret = confirmationAttributes["secret"] as? String
        if (secret == null) {
            confirmationCallback(false)
            return
        }

        val seed = confirmationAttributes["seed"] as? String
        if (seed == null) {
            confirmationCallback(false)
            return
        }

        val dialog = getStyledDialogBuilder()
            .setMessage(context.getString(R.string.template_tfa_add_dialog_message, secret))
            .setOnCancelListener {
                confirmationCallback(false)
            }
            .setPositiveButton(R.string.continue_action) { _, _ ->
                confirmationCallback(true)
            }
            .setNegativeButton(R.string.open_action, null)
            .setNeutralButton(R.string.copy_action, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            context.clipboardText = secret
            toastManager?.short(R.string.tfa_key_copied)
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            openAuthenticatorOrGooglePlay(seed)
        }
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