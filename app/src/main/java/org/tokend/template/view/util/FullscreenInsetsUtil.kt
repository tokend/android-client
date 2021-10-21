package org.tokend.template.view.util

import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import org.jetbrains.anko.dip

object FullscreenInsetsUtil {
    /**
     * @return height of navigation bar overlay in fullscreen
     */
    fun getNavigationBarOverlayHeight(viewToObtainInsets: View): Int {
        return ViewCompat.getRootWindowInsets(viewToObtainInsets)
            ?.systemWindowInsetBottom
            .let { height ->
                height
                    ?: if (Build.VERSION.SDK_INT in
                        (Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.LOLLIPOP_MR1)
                    )
                        viewToObtainInsets.context.dip(56)
                    else
                        0
            }
    }
}