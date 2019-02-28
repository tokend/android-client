package org.tokend.template.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.sdk.api.generated.resources.BalanceResource
import org.tokend.template.features.assets.model.AssetRecord
import java.io.Serializable
import java.math.BigDecimal

class BalanceRecord(
        val id: String,
        val asset: AssetRecord,
        val available: BigDecimal
): Serializable {
    constructor(source: BalanceResource, urlConfig: UrlConfig?, mapper: ObjectMapper): this(
            id = source.id,
            available = source.state.available,
            asset = AssetRecord.fromResource(source.asset, urlConfig, mapper)
    )

    val assetCode: String
        get() = asset.code
}