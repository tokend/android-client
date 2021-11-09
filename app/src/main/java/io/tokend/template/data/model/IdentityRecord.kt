package io.tokend.template.data.model

import org.tokend.sdk.api.identity.model.IdentityResource

/**
 * Holds account identity data
 */
data class IdentityRecord(
    val email: String,
    val accountId: String
) {
    constructor(source: IdentityResource) : this(
        email = source.email,
        accountId = source.address
    )
}