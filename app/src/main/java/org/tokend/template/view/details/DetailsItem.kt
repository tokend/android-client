package org.tokend.template.view.details

import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.view.View

/**
 * Preference-like item to display entity details
 */
class DetailsItem(
        val text: String? = null,
        val hint: String? = null,
        val icon: Drawable? = null,
        @ColorInt
        val textColor: Int? = null,
        /**
         * Section header
         */
        val header: String? = null,
        val singleLineText: Boolean = false,
        val isEnabled: Boolean = true,
        val extraView: View? = null,
        val id: Long = 0L
) {
    val hasHeader: Boolean = header != null

    override fun equals(other: Any?): Boolean {
        return other is DetailsItem && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}