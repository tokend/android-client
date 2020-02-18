package org.tokend.template.data.model

import com.fasterxml.jackson.databind.JsonNode
import org.tokend.sdk.api.generated.inner.ExternalSystemData
import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIDResource
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.wallet.xdr.ExternalSystemAccountIDPoolEntry
import java.io.Serializable
import java.util.*

class AccountRecord(
        val id: String,
        val kycRecoveryStatus: KycRecoveryStatus,
        val depositAccounts: MutableSet<DepositAccount>,
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
            depositAccounts = source.externalSystemIds?.map(::DepositAccount)?.toHashSet()
                    ?: mutableSetOf(),
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
                expirationDate = source.expiresAt,
                data = source.data
        )

        constructor(source: ExternalSystemAccountIDPoolEntry) : this(
                type = source.externalSystemType,
                expirationDate = Date(source.expiresAt * 1000L),
                data = JsonApiToolsProvider.getObjectMapper().readValue(
                        source.data,
                        ExternalSystemData::class.java
                )
        )

        constructor(type: Int, expirationDate: Date?, data: ExternalSystemData) : this(
                type = type,
                expirationDate = expirationDate,
                address = data.data.address,
                payload = data.data.payload
        )

        override fun hashCode(): Int =
                HashCodes.ofMany(type, address)

        override fun equals(other: Any?): Boolean =
                other is DepositAccount && other.type == this.type
                        && other.address == this.address
    }

    enum class KycRecoveryStatus {
        NONE,
        INITIATED,
        PENDING,
        REJECTED,
        PERMANENTLY_REJECTED;
    }

    fun getDepositAccount(asset: AssetRecord): DepositAccount? {
        val type =
                if (asset.isConnectedToCoinpayments)
                    asset.code.hashCode()
                else
                    asset.externalSystemType

        type ?: return null

        return depositAccounts
                .find { it.type == type }
    }
}