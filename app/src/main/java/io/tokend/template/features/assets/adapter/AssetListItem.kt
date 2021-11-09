package io.tokend.template.features.assets.adapter

import io.tokend.template.features.assets.model.AssetRecord

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