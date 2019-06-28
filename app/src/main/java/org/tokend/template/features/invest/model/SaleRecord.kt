package org.tokend.template.features.invest.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.generated.resources.SaleResource
import org.tokend.sdk.api.sales.model.SaleState
import org.tokend.sdk.api.sales.model.SimpleSale
import org.tokend.sdk.utils.extentions.isClosed
import org.tokend.sdk.utils.extentions.isEnded
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.model.UrlConfig
import org.tokend.wallet.xdr.SaleType
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

class SaleRecord(
        val id: Long,
        val name: String,
        val baseAsset: Asset,
        val quoteAssets: List<QuoteAsset>,
        val defaultQuoteAsset: Asset,
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
        val ownerAccountId: String
) : Serializable {

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
            override val code: String,
            override val trailingDigits: Int,
            val price: BigDecimal,
            val totalCurrentCap: BigDecimal,
            val hardCap: BigDecimal,
            val currentCap: BigDecimal,
            val softCap: BigDecimal,
            override val name: String? = null
    ) : Asset

    class YoutubeVideo(
            val url: String,
            val previewUrl: String
    ) : Serializable

    companion object {
        fun fromResource(source: SaleResource, urlConfig: UrlConfig?, mapper: ObjectMapper): SaleRecord {

            val name = source.details.get("name")?.takeIf { it !is NullNode }?.asText()
                    ?: ""

            val shortDescription = source.details.get("short_description")?.takeIf { it !is NullNode }?.asText()
                    ?: ""

            val fullDescription = source.details.get("description")?.takeIf { it !is NullNode }?.asText()
                    ?: ""

            val quoteAssetName = source.defaultQuoteAsset.asset.details
                    ?.get("name")?.takeIf { it !is NullNode }?.asText()

            val defaultQuoteAsset = QuoteAsset(
                    code = source.defaultQuoteAsset.asset.id,
                    trailingDigits = 6,
                    price = source.defaultQuoteAsset.price,
                    hardCap = source.defaultQuoteAsset.hardCap,
                    totalCurrentCap = source.defaultQuoteAsset.totalCurrentCap,
                    currentCap = source.defaultQuoteAsset.currentCap,
                    softCap = source.defaultQuoteAsset.softCap,
                    name = quoteAssetName
            )

            val quoteAssets =
                    source.quoteAssets.map {
                        QuoteAsset(
                                code = it.asset.id,
                                trailingDigits = 6,
                                price = it.price,
                                totalCurrentCap = it.totalCurrentCap,
                                hardCap = it.hardCap,
                                softCap = it.softCap,
                                currentCap = it.currentCap,
                                name = it.asset.details
                                        ?.get("name")?.takeIf { it !is NullNode }?.asText()
                        )
                    }

            val saleType = SaleType.values()
                    .find { it.value == source.saleType.value } ?: SaleType.BASIC_SALE

            val logo = source.details.get("logo")?.takeIf { it !is NullNode }?.let {
                mapper.convertValue(it, RemoteFile::class.java)
            }

            val youtubeVideo = source.details.get("youtube_video_id")
                    ?.takeIf { it !is NullNode  }
                    ?.asText()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                YoutubeVideo(
                        url = "https://m.youtube.com/watch?v=$it",
                        previewUrl = "https://img.youtube.com/vi/$it/hqdefault.jpg"
                )
            }

            return SaleRecord(
                    id = source.id.toLong(),
                    name = name,
                    baseAsset = SimpleAsset(source.baseAsset),
                    quoteAssets = quoteAssets,
                    defaultQuoteAsset = defaultQuoteAsset,
                    shortDescription = shortDescription,
                    fullDescriptionBlob = fullDescription,
                    logoUrl = logo?.getUrl(urlConfig?.storage),
                    startDate = source.startTime,
                    endDate = source.endTime,
                    state = SaleState.fromValue(source.saleState.value),
                    type = saleType,
                    baseHardCap = source.baseHardCap,
                    currentCap = defaultQuoteAsset.currentCap,
                    softCap = defaultQuoteAsset.softCap,
                    hardCap = defaultQuoteAsset.hardCap,
                    ownerAccountId = source.owner.id,
                    youtubeVideo = youtubeVideo
            )
        }
    }
}