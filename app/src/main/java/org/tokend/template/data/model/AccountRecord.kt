package org.tokend.template.data.model

import com.fasterxml.jackson.databind.JsonNode
import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIDResource
import java.io.Serializable
import java.util.*

class AccountRecord(
        val id: String,
        val kycRecoveryStatus: KycRecoveryStatus,
        val depositAccounts: List<DepositAccount>,
        val kycBlob: String?
) : Serializable {
    constructor(source: AccountResource) : this(
            id = source.id,
            kycRecoveryStatus = source
                    .kycRecoveryStatus
                    ?.name
                    ?.toUpperCase(Locale.ENGLISH)
                    ?.let(KycRecoveryStatus::valueOf)
                    ?: KycRecoveryStatus.NONE,
            depositAccounts = source.externalSystemIds?.map(::DepositAccount) ?: emptyList(),
            kycBlob = source
                    .kycData
                    ?.kycData
                    ?.get("blob_id")
                    ?.takeIf(JsonNode::isTextual)
                    ?.asText()
    )

    class DepositAccount(
            val type: Int,
            val address: String,
            val payload: String?,
            val expirationDate: Date?
    ) : Serializable {
        constructor(source: ExternalSystemIDResource) : this(
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