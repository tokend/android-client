package org.tokend.template.features.assets.adapter

import org.tokend.template.data.model.AssetRecord

class AssetListItem(
        val code: String,
        val name: String?,
        val balanceId: String?,
        val logoUrl: String?,
        val source: AssetRecord
) {
    val balanceExists: Boolean = balanceId != null

    constructor(
            asset: AssetRecord,
            balanceId: String?
    ) : this(
            code = asset.code,
            name = asset.name,
            logoUrl = asset.logoUrl,
            balanceId = balanceId,
            source = asset
    )
}