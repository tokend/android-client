package org.tokend.template.test

import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.template.view.util.RemainedTimeUtil
import java.util.*
import java.util.concurrent.TimeUnit

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RemainedTimeUtilTest {
    @Test
    fun aDays() {
        val days = 3
        val date = Date(Date().time + days * 24 * 3600 * 1000)

        val result = RemainedTimeUtil.getRemainedTime(date)

        Assert.assertEquals(days, result.value)
        Assert.assertEquals(TimeUnit.DAYS, result.unit)
    }

    @Test
    fun bHours() {
        val hours = 2
        val date = Date(Date().time + hours * 3600 * 1000)

        val result = RemainedTimeUtil.getRemainedTime(date)

        Assert.assertEquals(hours, result.value)
        Assert.assertEquals(TimeUnit.HOURS, result.unit)
    }

    @Test
    fun cMinutes() {
        val minutes = 2
        val date = Date(Date().time + minutes * 60 * 1000)

        val result = RemainedTimeUtil.getRemainedTime(date)

        Assert.assertEquals(minutes, result.value)
        Assert.assertEquals(TimeUnit.MINUTES, result.unit)
    }

    @Test
    fun dLessThanMinute() {
        val date = Date(Date().time + 10 * 1000)

        val result = RemainedTimeUtil.getRemainedTime(date)

        Assert.assertEquals(0, result.value)
        Assert.assertEquals(TimeUnit.MINUTES, result.unit)
    }
}