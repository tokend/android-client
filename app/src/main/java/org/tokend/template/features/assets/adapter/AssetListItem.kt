package org.tokend.template.features.assets.adapter

import org.tokend.template.extensions.Asset

class AssetListItem(
        val code: String,
        val name: String?,
        val balanceExists: Boolean,
        val logoUrl: String?,
        val source: Asset
) {
    constructor(
            asset: Asset,
            balanceExists: Boolean,
            storageUrl: String
    ) : this(
            code = asset.code,
            name = asset.details.name,
            logoUrl = asset.details.logo.let { logo ->
                logo.getUrl(storageUrl)?.takeIf { logo.isImage }
            },
            balanceExists = balanceExists,
            source = asset
    )
}