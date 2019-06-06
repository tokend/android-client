package org.tokend.template.data.model

import com.fasterxml.jackson.databind.node.NullNode
import org.tokend.sdk.api.generated.resources.AssetResource

class SimpleAsset(
        override val code: String,
        override val trailingDigits: Int,
        override val name: String?
) : Asset {

    constructor(source: AssetResource) : this(
            code = source.id,
            trailingDigits = source.trailingDigits.toInt(),
            name = source.details.get("name")?.takeIf { it !is NullNode }?.asText()
    )

    @Deprecated("Going to be removed. Right now used because of some issues")
    constructor(asset: String) : this(
            code = asset,
            trailingDigits = 6,
            name = null
    )
}