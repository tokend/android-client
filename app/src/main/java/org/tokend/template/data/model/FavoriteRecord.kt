package org.tokend.template.data.model

import org.tokend.sdk.api.favorites.model.FavoriteEntry

open class FavoriteRecord(
        val type: String,
        val key: String,
        val id: Long
) {
    constructor(source: FavoriteEntry) : this(
            type = source.type,
            key = source.key,
            id = source.id
    )

    override fun equals(other: Any?): Boolean {
        return other is FavoriteRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        const val TYPE_SALE = "sale"
        const val TYPE_ASSET_PAIR = "asset_pair"

        fun sale(asset: String, id: Long = 0) = FavoriteRecord(TYPE_SALE, asset, id)
        fun assetPair(base: String, quote: String, id: Long = 0) = FavoriteRecord(TYPE_ASSET_PAIR, "$base-$quote", id)
    }
}