package org.tokend.template.view.dialog

import android.content.Context
import android.graphics.Typeface
import android.support.v7.app.AlertDialog
import android.widget.TextView
import org.jetbrains.anko.isSelectable
import org.tokend.template.R
import org.tokend.template.di.providers.AccountProvider
import java.nio.CharBuffer

class SecretSeedDialog(
        private val context: Context,
        private val accountProvider: AccountProvider
) {

    fun show() {
        showSeedWarningDialog()
    }

    private fun showSeedWarningDialog() {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setMessage(context.getString(R.string.seed_alert_dialog))
                .setNegativeButton(context.getString(R.string.cancel), null)
                .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                    showSecretSeed()
                }.show()
    }

    private fun showSecretSeed() {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setMessage(
                        CharBuffer.wrap(accountProvider.getAccount()?.secretSeed
                                ?: context.getString(R.string.error_try_again).toCharArray())
                )
                .setTitle(R.string.secret_seed)
                .setPositiveButton(R.string.ok, null)
                .show().findViewById<TextView>(android.R.id.message)?.let { textView ->
                    textView.isSelectable = true
                    textView.typeface = Typeface.MONOSPACE
                }
    }
}