package org.tokend.template.features.tfa.model

import org.tokend.sdk.api.tfa.model.TfaFactor

class TotpTfaFactorRecord : TfaFactorRecord {
    val seed: String
    val secret: String

    constructor(type: TfaFactor.Type,
                priority: Int,
                id: Long,
                seed: String,
                secret: String
    ) : super(type, priority, id) {
        this.secret = secret
        this.seed = seed
    }

    constructor(source: TfaFactor) : super(source) {
        this.seed = source.attributes.seed
                ?: throw IllegalArgumentException("TOTP seed is required")
        this.secret = source.attributes.secret
                ?: throw IllegalArgumentException("TOTP secret is required")
    }
}