package org.tokend.template.features.explore.adapter

import org.tokend.template.BuildConfig
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
            balanceExists: Boolean
    ) : this(
            code = asset.code,
            name = asset.details?.name,
            logoUrl = asset.details?.logo.let { logo ->
                logo?.getUrl(BuildConfig.STORAGE_URL)?.takeIf { logo.isImage }
            },
            balanceExists = balanceExists,
            source = asset
    )
}