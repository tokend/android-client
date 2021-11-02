package org.tokend.template.view.details

import android.content.Context
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import android.widget.Button
import org.tokend.template.R

/***
 * Provides extra views for [org.tokend.template.view.details.DetailsItem]
 */

object ExtraViewProvider {
    fun getFeeView(context: Context, action: () -> Unit): AppCompatImageView {
        return AppCompatImageView(context).apply {
            val padding = context.resources.getDimensionPixelSize(R.dimen.half_standard_margin)
            setPadding(padding, padding, padding, padding)
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_help_circle).apply {
                val color = ContextCompat.getColor(context, R.color.secondary_text)
                setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
            setImageDrawable(drawable)
            setOnClickListener { action.invoke() }
        }
    }

    fun getButton(context: Context, textRes: Int, action: () -> Unit): Button {
        val newContext = ContextThemeWrapper(context, R.style.PrimaryButton)
        return Button(newContext, null, R.style.PrimaryButton).apply {
            text = context.getString(textRes)
            setOnClickListener { action.invoke() }
        }
    }
}