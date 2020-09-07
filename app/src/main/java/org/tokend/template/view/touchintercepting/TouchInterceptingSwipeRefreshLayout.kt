package org.tokend.template.view.touchintercepting

import android.content.Context
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * [SwipeRefreshLayout] with ability to set interceptor for [onInterceptTouchEvent]
 *
 * @see setTouchEventInterceptor
 */
class TouchInterceptingSwipeRefreshLayout : SwipeRefreshLayout {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context) : super(context)

    private var touchEventInterceptor: ((e: MotionEvent) -> Boolean)? = null

    /**
     * Sets interceptor for [onInterceptTouchEvent]
     */
    fun setTouchEventInterceptor(interceptor: ((e: MotionEvent) -> Boolean)?) {
        this.touchEventInterceptor = interceptor
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val handled = touchEventInterceptor?.invoke(ev) ?: false
        return if (handled) true else super.onInterceptTouchEvent(ev)
    }
}