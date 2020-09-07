package org.tokend.template.view.util.fab

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import android.view.ContextThemeWrapper
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import org.tokend.template.R

/**
 * Represents small [FloatingActionButton], an element of [FloatingActionMenu].
 */
class FloatingActionMenuAction(
        val title: String,
        val icon: Drawable,
        val onClickListener: () -> Unit,
        val isEnabled: Boolean = true,
        val id: Int = 0
) {
    constructor(context: Context,
                @StringRes
                titleRes: Int,
                @DrawableRes
                iconRes: Int,
                onClickListener: () -> Unit,
                isEnabled: Boolean = true,
                id: Int = 0
    ) : this(
            title = context.getString(titleRes),
            icon = ContextCompat.getDrawable(context, iconRes)
                    ?: throw IllegalArgumentException("Drawable $iconRes not found"),
            onClickListener = onClickListener,
            isEnabled = isEnabled,
            id = id
    )

    /**
     * @return FloatingActionButton with all corresponding attributes
     * and accessible by [id]
     */
    fun toButton(context: Context,
                 @StyleRes style: Int = R.style.FloatingButtonMenuItem
    ): FloatingActionButton {
        val themedContext = ContextThemeWrapper(context, style)

        return FloatingActionButton(themedContext, null, R.style.FloatingButtonMenuItem)
                .apply {
                    id = this@FloatingActionMenuAction.id
                    labelText = this@FloatingActionMenuAction.title
                    setImageDrawable(this@FloatingActionMenuAction.icon)
                    isEnabled = this@FloatingActionMenuAction.isEnabled
                    setOnClickListener {
                        onClickListener()
                        (parent as? FloatingActionMenu)?.close(false)
                    }
                }
    }
}