package org.tokend.template.data.model

import java.math.BigDecimal
import java.util.*

class AssetChartData(
        val hour: List<AssetChartPoint>? = null,
        val day: List<AssetChartPoint>? = null,
        val month: List<AssetChartPoint>? = null,
        val year: List<AssetChartPoint>? = null
) {
    class AssetChartPoint(
            val value: BigDecimal,
            val date: Date
    ) {
        constructor(source: org.tokend.sdk.api.assets.model.AssetChartData.ChartPoint) : this(
                value = source.value ?: BigDecimal.ZERO,
                date = source.date
        )
    }

    constructor(source: org.tokend.sdk.api.assets.model.AssetChartData) : this(
            hour = source.hour?.map(::AssetChartPoint),
            day = source.day?.map(::AssetChartPoint),
            month = source.month?.map(::AssetChartPoint),
            year = source.year?.map(::AssetChartPoint)
    )

    val isEmpty: Boolean
        get() = hour.isNullOrEmpty()
                && day.isNullOrEmpty()
                && month.isNullOrEmpty()
                && year.isNullOrEmpty()
}