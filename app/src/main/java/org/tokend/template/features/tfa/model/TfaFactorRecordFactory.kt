package org.tokend.template.features.tfa.model

import org.tokend.sdk.api.tfa.model.TfaFactor

class TfaFactorRecordFactory {
    /**
     * Creates [TfaFactorRecord] from [TfaFactor] returned on factor creation.
     * In this case it has special attributes required for factor confirmation.
     */
    fun fromFactorCreationResponse(source: TfaFactor): TfaFactorRecord {
        return when (source.type) {
            TfaFactor.Type.TOTP -> TotpTfaFactorRecord(source)
            else -> TfaFactorRecord(source)
        }
    }
}