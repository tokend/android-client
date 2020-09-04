package org.tokend.template.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import org.jetbrains.anko.clipboardManager
import org.tokend.template.R
import org.tokend.template.view.ToastManager

object CopyDataDialogFactory {

    fun getDialog(
            context: Context,
            content: CharSequence,
            title: String?,
            toastManager: ToastManager,
            toastMessage: String = context.getString(R.string.data_has_been_copied)
    ): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.copy_action, null)
                .show()
                .apply {
                    findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)

                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        context.clipboardManager.text = content
                        toastManager.short(toastMessage)
                    }
                }
    }
}