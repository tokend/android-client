package io.tokend.template.view.util

import android.view.GestureDetector
import android.view.MotionEvent

class HorizontalSwipesGestureDetector(
    private val onSwipeToLeft: (() -> Unit)? = null,
    private val onSwipeToRight: (() -> Unit)? = null
) : GestureDetector.SimpleOnGestureListener() {
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return processFling(e1, e2, velocityX)
    }

    private fun processFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float
    ): Boolean {
        e1 ?: return false
        e2 ?: return false

        val distanceX = e2.x - e1.x
        val distanceY = e2.y - e1.y

        val absDistanceX = Math.abs(distanceX)
        val absDistanceY = Math.abs(distanceY)
        val absVelocity = Math.abs(velocityX)

        if (absDistanceX > absDistanceY
            && absDistanceX > SWIPE_DISTANCE_THRESHOLD
            && absVelocity > SWIPE_VELOCITY_THRESHOLD
        ) {
            if (distanceX > 0)
                onSwipeToRightDetected()
            else
                onSwipeToLeftDetected()
            return true
        }
        return false
    }

    private fun onSwipeToLeftDetected() {
        onSwipeToLeft?.invoke()
    }

    private fun onSwipeToRightDetected() {
        onSwipeToRight?.invoke()
    }

    private companion object {
        const val SWIPE_DISTANCE_THRESHOLD = 100
        const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}