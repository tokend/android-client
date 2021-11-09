package io.tokend.template.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.tokend.template.R

object SignOutDialogFactory {

    fun getDialog(context: Context, action: (() -> Unit)? = null): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setMessage(R.string.sign_out_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                action?.invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }
}