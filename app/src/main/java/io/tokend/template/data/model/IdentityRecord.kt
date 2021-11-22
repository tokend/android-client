package io.tokend.template.data.model

import org.tokend.sdk.api.identity.model.IdentityResource

/**
 * Holds account identity data
 */
data class IdentityRecord(
    val login: String,
    val accountId: String
) {
    constructor(source: IdentityResource) : this(
        login = source.email,
        accountId = source.address
    )
}