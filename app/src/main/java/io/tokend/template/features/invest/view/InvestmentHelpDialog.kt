package io.tokend.template.features.invest.view

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.widget.Scroller
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import io.tokend.template.R

class InvestmentHelpDialog(
    context: Context,
    @StyleRes
    style: Int? = null
) {
    private val dialog: AlertDialog

    init {
        val builder =
            if (style != null)
                AlertDialog.Builder(context, style)
            else
                AlertDialog.Builder(context)

        dialog = builder
            .setTitle(R.string.investment_help_title)
            .setMessage(R.string.investment_help_content)
            .setPositiveButton(R.string.ok, null)
            .create()
    }

    fun show(): AlertDialog {
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.apply {
            setScroller(Scroller(dialog.context))
            isVerticalScrollBarEnabled = true
            movementMethod = ScrollingMovementMethod()
        }
        return dialog
    }
}