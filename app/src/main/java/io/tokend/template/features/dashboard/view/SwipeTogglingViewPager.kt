package io.tokend.template.features.dashboard.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * [ViewPager] without horizontal swipes
 */
class SwipeTogglingViewPager
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    androidx.viewpager.widget.ViewPager(context, attrs) {
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