package org.tokend.template.view.util

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R

object ProgressDialogFactory {

    fun getDialog(context: Context,
                  @StringRes
                  messageRes: Int = R.string.processing_progress,
                  cancelListener: ((DialogInterface) -> Unit)? = null
    ): AlertDialog {
        val view = context.layoutInflater.inflate(R.layout.progress_dialog, null, false)
        view.findViewById<TextView>(R.id.message).setText(messageRes)
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setView(view)
                .apply {
                    if (cancelListener != null) {
                        setCancelable(true)
                        setOnCancelListener(cancelListener)
                    } else {
                        setCancelable(false)
                    }
                }
                .create()
    }
}