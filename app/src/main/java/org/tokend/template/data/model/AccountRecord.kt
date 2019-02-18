package org.tokend.template.data.model

import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIdResource
import org.tokend.wallet.xdr.AccountType
import java.io.Serializable
import java.util.*

class AccountRecord(
        val id: String,
        val type: AccountType,
        val depositAccounts: List<DepositAccount>
): Serializable {
    constructor(source: AccountResource) : this(
            id = source.id,
            // TODO: Figure out what to do
            type = AccountType.VERIFIED,
            depositAccounts = source.externalSystemIds.map(::DepositAccount)
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