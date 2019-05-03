package org.tokend.template.view.util

import android.content.Context
import android.graphics.PorterDuff
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import org.jetbrains.anko.dimen
import org.jetbrains.anko.onClick
import org.jetbrains.anko.padding
import org.tokend.template.R

/***
 * Provides extra views for [org.tokend.template.view.details.DetailsItem]
 */

object ExtraViewProvider {
    fun getFeeView(context: Context, action: () -> Unit): AppCompatImageView {
        return AppCompatImageView(context).apply {
            padding = context.dimen(R.dimen.half_standard_margin)
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_help_circle).apply {
                val color = ContextCompat.getColor(context, R.color.secondary_text)
                setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
            setImageDrawable(drawable)
            onClick { action.invoke() }
        }
    }
}