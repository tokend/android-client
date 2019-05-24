package org.tokend.template.features.wallet.view

import android.content.Context
import android.support.design.widget.CollapsingToolbarLayout
import android.util.AttributeSet

/**
 * [CollapsingToolbarLayout] with callback on scrim visibility change
 */
class ScrimCallbackCollapsingToolbarLayout
@JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CollapsingToolbarLayout(context, attrs, defStyleAttr) {

    /**
     * Receives scrim visibility as a param
     */
    var scrimCallback: ((Boolean) -> Unit)? = null

    private var scrimIsShown = false

    override fun setScrimsShown(shown: Boolean, animate: Boolean) {
        super.setScrimsShown(shown, animate)

        if (scrimIsShown != shown) {
            scrimCallback?.invoke(shown)
        }

        scrimIsShown = shown
    }
}