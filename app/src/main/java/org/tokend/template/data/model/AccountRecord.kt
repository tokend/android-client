package org.tokend.template.data.model

import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIdResource
import java.io.Serializable
import java.util.*

class AccountRecord(
        val id: String,
        val depositAccounts: List<DepositAccount>
): Serializable {
    constructor(source: AccountResource) : this(
            id = source.id,
            depositAccounts = source.externalSystemIds?.map(::DepositAccount) ?: emptyList()
    )

    class DepositAccount(
            val type: Int,
            val address: String,
            val expirationDate: Date?
    ): Serializable {
        constructor(source: ExternalSystemIdResource) : this(
                type = source.externalSystemType,
                address = source.data,
                expirationDate = source.expiresAt
        )
    }
}