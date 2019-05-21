package org.tokend.template.features.dashboard.view

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * [ViewPager] without horizontal swipes
 */
class SwipeTogglingViewPager
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    var swipesEnabled: Boolean = true

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return if (swipesEnabled)
            super.onTouchEvent(ev)
        else
            false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (swipesEnabled)
            super.onInterceptTouchEvent(ev)
        else
            false
    }
}