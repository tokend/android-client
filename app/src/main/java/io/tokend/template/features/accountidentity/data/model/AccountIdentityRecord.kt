package io.tokend.template.features.accountidentity.data.model

import org.tokend.sdk.api.identity.model.IdentityResource

/**
 * Holds account identity data
 */
data class AccountIdentityRecord(
    val login: String,
    val accountId: String,
) {
    constructor(source: IdentityResource) : this(
        login = source.email,
        accountId = source.address,
    )
}