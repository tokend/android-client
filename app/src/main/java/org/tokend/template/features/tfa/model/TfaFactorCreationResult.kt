package org.tokend.template.features.tfa.model

data class TfaFactorCreationResult(
        val newFactor: TfaFactorRecord,
        val confirmationAttributes: Map<String, Any>
)