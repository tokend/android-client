package io.tokend.template.features.tfa.model

import org.tokend.sdk.api.tfa.model.TfaFactor

open class TfaFactorRecord(
    val type: TfaFactor.Type,
    var priority: Int,
    val id: Long = 0L
) {
    constructor(source: TfaFactor) : this(
        id = source.id,
        priority = source.attributes.priority,
        type = source.type
    )
}