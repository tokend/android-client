package io.tokend.template.features.assets.model

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
        constructor(source: org.tokend.sdk.api.charts.model.AssetChartData.ChartPoint) : this(
            value = source.value,
            date = source.date
        )
    }

    constructor(source: org.tokend.sdk.api.charts.model.AssetChartData) : this(
        hour = source.hour?.map(AssetChartData::AssetChartPoint),
        day = source.day?.map(AssetChartData::AssetChartPoint),
        month = source.month?.map(AssetChartData::AssetChartPoint),
        year = source.year?.map(AssetChartData::AssetChartPoint)
    )

    val isEmpty: Boolean
        get() = hour.isNullOrEmpty()
                && day.isNullOrEmpty()
                && month.isNullOrEmpty()
                && year.isNullOrEmpty()
}