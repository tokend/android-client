package org.tokend.template.view.util

import android.content.Context
import android.support.v7.app.AlertDialog
import org.tokend.template.R

object SignOutDialogFactory {

    fun getTunedDialog(context: Context, action: (() -> Unit)? = null): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setMessage(R.string.sign_out_confirmation)
                .setPositiveButton(R.string.yes) { _, _ ->
                    action?.invoke()
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
    }
}