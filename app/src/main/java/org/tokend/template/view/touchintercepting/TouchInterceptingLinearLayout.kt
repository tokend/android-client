package org.tokend.template.view.touchintercepting

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

/**
 * [LinearLayout] with ability to set interceptor for [onInterceptTouchEvent]
 *
 * @see setTouchEventInterceptor
 */
class TouchInterceptingLinearLayout : LinearLayout {
    constructor(context: Context, attributeSet: AttributeSet?) :
            super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) :
            super(context, attributeSet, style)

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