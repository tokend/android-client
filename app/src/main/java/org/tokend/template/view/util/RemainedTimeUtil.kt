package org.tokend.template.view.util

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object RemainedTimeUtil {
    data class RemainedTime(
            val value: Int,
            val unit: TimeUnit
    )

    /**
     * @return most suitable [TimeUnit] and it's amount i.e. "1 day", "4 hours", "3 minutes"
     */
    fun getRemainedTime(toDate: Date): RemainedTime {
        val milliseconds = toDate.time - Date().time

        val (timeValue, timeUnit) =
                ((milliseconds / (1000f * 86400)).roundToInt() to TimeUnit.DAYS)
                        .takeIf { it.first > 0 }

                        ?: ((milliseconds / (1000f * 3600)).roundToInt() to TimeUnit.HOURS)
                                .takeIf { it.first > 0 }

                        ?: ((milliseconds / (1000f * 60)).roundToInt() to TimeUnit.MINUTES)

        return RemainedTime(
                timeValue,
                timeUnit
        )
    }
}