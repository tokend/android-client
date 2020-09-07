package org.tokend.template.features.localaccount.mnemonic.view

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import org.tokend.template.R
import org.tokend.template.view.ToastManager
import org.tokend.template.view.dialog.CopyDataDialogFactory

class MnemonicPhraseDialog(
        private val context: Context,
        private val phrase: String,
        private val toastManager: ToastManager,
        @StyleRes
        private val style: Int = R.style.AlertDialogStyle
) {
    fun show() {
        showSensitiveDataWarningDialog()
    }

    private fun showSensitiveDataWarningDialog() {
        AlertDialog.Builder(context, style)
                .setMessage(context.getString(R.string.mnemonic_alert_dialog))
                .setNegativeButton(context.getString(R.string.no), null)
                .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                    showMnemonic()
                }.show()
    }

    private fun showMnemonic() {
        CopyDataDialogFactory.getDialog(
                context = context,
                content = phrase,
                title = context.getString(R.string.mnemonic_phrase),
                toastManager = toastManager,
                toastMessage = context.getString(R.string.data_has_been_copied)
        )
    }
}