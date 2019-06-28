package org.tokend.template.features.invest.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.generated.resources.SaleQuoteAssetResource
import org.tokend.sdk.api.generated.resources.SaleResource
import org.tokend.sdk.api.sales.model.SaleState
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
        val fullDescriptionBlob: String?,
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
            val softCap: BigDecimal
    ) : Asset {
        override val name: String? = null

        constructor(source: SaleQuoteAssetResource) : this(
                code = source.asset.id,
                trailingDigits = 6,
                price = source.price,
                hardCap = source.hardCap,
                totalCurrentCap = source.totalCurrentCap,
                currentCap = source.currentCap,
                softCap = source.softCap
        )
    }

    class YoutubeVideo(
            val url: String,
            val previewUrl: String
    ) : Serializable

    companion object {
        fun fromResource(source: SaleResource, urlConfig: UrlConfig?, mapper: ObjectMapper): SaleRecord {

            val name = source.details.get("name").asText()

            val shortDescription = source.details.get("short_description")
                    ?.takeIf { it !is NullNode }
                    ?.asText()
                    ?: ""

            val fullDescription = source.details.get("description")?.takeIf { it !is NullNode }
                    ?.asText()

            val defaultQuoteAsset = QuoteAsset(source.defaultQuoteAsset)
            val quoteAssets = source.quoteAssets.map(::QuoteAsset)

            val saleType = SaleType.values()
                    .find { it.value == source.saleType.value }
                    ?: throw IllegalArgumentException("Unknown sale type ${source.saleType.value}")

            val logo = source.details.get("logo")?.takeIf { it !is NullNode }?.let {
                mapper.convertValue(it, RemoteFile::class.java)
            }

            val youtubeVideo = source.details.get("youtube_video_id")
                    ?.takeIf { it !is NullNode }
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