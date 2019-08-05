package org.tokend.template.data.model

import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIdResource
import java.io.Serializable
import java.util.*

class AccountRecord(
        val id: String,
        val kycRecoveryStatus: KycRecoveryStatus,
        val depositAccounts: List<DepositAccount>
) : Serializable {
    constructor(source: AccountResource) : this(
            id = source.id,
            kycRecoveryStatus = KycRecoveryStatus.valueOf(source.kycRecoveryStatus.name.toUpperCase()),
            depositAccounts = source.externalSystemIds?.map(::DepositAccount) ?: emptyList()
    )

    class DepositAccount(
            val type: Int,
            val address: String,
            val payload: String?,
            val expirationDate: Date?
    ) : Serializable {
        constructor(source: ExternalSystemIdResource) : this(
                type = source.externalSystemType,
                address = source.data.data.address,
                payload = source.data.data.payload,
                expirationDate = source.expiresAt
        )
    }

    enum class KycRecoveryStatus {
        NONE,
        INITIATED,
        PENDING,
        REJECTED,
        PERMANENTLY_REJECTED;
    }
}