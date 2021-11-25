package io.tokend.template.view.util

import android.app.Activity
import android.util.DisplayMetrics
import io.tokend.template.R
import kotlin.math.ceil

object ColumnCalculator {
    fun getColumnCount(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels.toDouble()
        return ceil((screenWidth / activity.resources.getDimensionPixelSize(R.dimen.max_content_width)))
            .toInt()
    }
}