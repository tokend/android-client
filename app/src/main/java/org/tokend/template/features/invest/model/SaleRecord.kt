package org.tokend.template.features.invest.model

import org.tokend.sdk.api.sales.model.SaleState
import org.tokend.sdk.api.sales.model.SimpleSale
import org.tokend.template.data.model.UrlConfig
import org.tokend.wallet.xdr.SaleType
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class SaleRecord(
        val id: Long,
        val name: String,
        val baseAssetCode: String,
        val quoteAssets: List<QuoteAsset>,
        val defaultQuoteAsset: String,
        val shortDescription: String,
        val fullDescriptionBlob: String,
        val logoUrl: String?,
        val startDate: Date,
        val endDate: Date,
        val state: SaleState,
        val type: SaleType,
        val softCap: BigDecimal,
        val hardCap: BigDecimal,
        val baseHardCap: BigDecimal,
        val currentCap: BigDecimal,
        val youtubeVideo: YoutubeVideo?,
        val investorsCount: Int
) : Serializable {
    constructor(source: SimpleSale,
                urlConfig: UrlConfig?
    ) : this(
            id = source.id,
            name = source.details.name,
            baseAssetCode = source.baseAsset,
            defaultQuoteAsset = source.defaultQuoteAsset,
            quoteAssets = source.quoteAssets.map {
                QuoteAsset(
                        code = it.code,
                        price = it.price,
                        hardCap = it.hardCap,
                        totalCurrentCap = it.totalCurrentCap
                )
            },
            shortDescription = source.details.shortDescription,
            fullDescriptionBlob = source.details.descriptionBlob,
            logoUrl = source.details.logo.getUrl(urlConfig?.storage),
            startDate = source.startDate,
            endDate = source.endDate,
            state = SaleState.fromName(source.state.name),
            type = SaleType.values().find { it.value == source.type.value }!!,
            softCap = source.softCap,
            hardCap = source.hardCap,
            baseHardCap = source.baseHardCap,
            currentCap = source.currentCap,
            youtubeVideo =
            try {
                YoutubeVideo(source.details.getYoutubeVideoUrl(true)!!,
                        source.details.youtubeVideoPreviewImage!!)
            } catch (_: Exception) {
                null
            },
            investorsCount = source.statistics.investors
    )

    val isAvailable: Boolean
        get() = !isUpcoming && !isEnded

    val isUpcoming: Boolean
        get() = startDate.after(Date())

    val isEnded: Boolean
        get() = isClosed || isCanceled

    val isClosed: Boolean
        get() = state == SaleState.CLOSED

    val isCanceled: Boolean
        get() = state == SaleState.CANCELED

    override fun equals(other: Any?): Boolean {
        return other is SaleRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    class QuoteAsset(
            val code: String,
            val price: BigDecimal,
            val hardCap: BigDecimal,
            val totalCurrentCap: BigDecimal
    ) : Serializable

    class YoutubeVideo(
            val url: String,
            val previewUrl: String
    ) : Serializable
}