package org.tokend.template.features.assets.adapter

import org.tokend.template.features.assets.model.AssetRecord

class AssetListItem(
        val code: String,
        val name: String?,
        val balanceExists: Boolean,
        val logoUrl: String?,
        val source: AssetRecord
) {
    constructor(
            asset: AssetRecord,
            balanceExists: Boolean,
            storageUrl: String
    ) : this(
            code = asset.code,
            name = asset.name,
            logoUrl = asset.logoUrl,
            balanceExists = balanceExists,
            source = asset
    )
}