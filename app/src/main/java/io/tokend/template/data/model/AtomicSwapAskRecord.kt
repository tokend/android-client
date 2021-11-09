package io.tokend.template.data.model

import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import org.tokend.sdk.api.generated.resources.AtomicSwapAskResource
import org.tokend.sdk.api.generated.resources.AtomicSwapQuoteAssetResource
import java.io.Serializable
import java.math.BigDecimal

class AtomicSwapAskRecord(
    val id: String,
    val asset: Asset,
    val amount: BigDecimal,
    val isCanceled: Boolean,
    val quoteAssets: List<QuoteAsset>
) : Serializable {
    constructor(source: AtomicSwapAskResource) : this(
        id = source.id,
        asset = SimpleAsset(source.baseAsset),
        amount = source.availableAmount,
        isCanceled = source.isCanceled,
        quoteAssets = source.quoteAssets.map(::QuoteAsset)
    )

    override fun equals(other: Any?): Boolean {
        return other is AtomicSwapAskRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    class QuoteAsset(
        override val code: String,
        override val trailingDigits: Int,
        val price: BigDecimal
    ) : Asset {
        override val name: String? = null

        constructor(source: AtomicSwapQuoteAssetResource) : this(
            code = source.quoteAsset,
            price = source.price,
            trailingDigits = 6 // Well..
        )
    }
}