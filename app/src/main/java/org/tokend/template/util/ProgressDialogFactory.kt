package org.tokend.template.util

import android.app.ProgressDialog
import android.content.Context
import org.tokend.template.R

object ProgressDialogFactory {

    fun getTunedDialog(context: Context?): ProgressDialog {
        val dialog = ProgressDialog(context)
        dialog.isIndeterminate = true
        dialog.setMessage(context?.getString(R.string.processing_progress))
        dialog.setCancelable(false)
        return dialog
    }
}