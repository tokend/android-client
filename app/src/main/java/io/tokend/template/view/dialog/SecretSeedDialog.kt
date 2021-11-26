package io.tokend.template.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.tokend.template.R
import io.tokend.template.view.ToastManager
import org.tokend.wallet.Account
import java.nio.CharBuffer

class SecretSeedDialog(
    private val context: Context,
    private val account: Account,
    private val toastManager: ToastManager
) {

    fun show() {
        showSeedWarningDialog()
    }

    private fun showSeedWarningDialog() {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setMessage(context.getString(R.string.seed_alert_dialog))
            .setNegativeButton(context.getString(R.string.no), null)
            .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                showSecretSeed()
            }.show()
    }

    private fun showSecretSeed() {
        val seed = account.secretSeed

        CopyDataDialogFactory.getDialog(
            context = context,
            content = CharBuffer.wrap(seed),
            title = context.getString(R.string.secret_seed),
            toastManager = toastManager,
            toastMessage = context.getString(R.string.data_has_been_copied)
        )
    }
}