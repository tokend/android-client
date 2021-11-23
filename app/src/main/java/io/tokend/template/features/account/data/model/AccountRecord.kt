package io.tokend.template.features.account.data.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.sdk.api.generated.inner.ExternalSystemData
import org.tokend.sdk.api.generated.resources.AccountResource
import org.tokend.sdk.api.generated.resources.ExternalSystemIDResource
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.wallet.xdr.ExternalSystemAccountIDPoolEntry
import java.io.Serializable
import java.util.*

class AccountRecord(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("role")
    var role: ResolvedAccountRole,
    @JsonProperty("kyc_recovery_status")
    var kycRecoveryStatus: KycRecoveryStatus,
    @JsonProperty("kyc_blob_id")
    val kycBlob: String?,
    @JsonProperty("deposit_accounts")
    val depositAccounts: MutableSet<DepositAccount>
) : Serializable {

    class DepositAccount(
        @JsonProperty("type")
        val type: Int,
        @JsonProperty("address")
        val address: String,
        @JsonProperty("payload")
        val payload: String?,
        @JsonProperty("expiration_date")
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DepositAccount

            if (type != other.type) return false
            if (address != other.address) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type
            result = 31 * result + address.hashCode()
            return result
        }
    }

    /**
     * @param keyValueEntries role-related key-value entries to resolve [AccountRole]
     *
     * @see NoSuchAccountRoleException
     */
    constructor(
        source: AccountResource,
        keyValueEntries: Collection<KeyValueEntryRecord>
    ) : this(
        id = source.id,
        role = ResolvedAccountRole(source.role.id.toLong(), keyValueEntries),
        kycRecoveryStatus = source
            .kycRecoveryStatus
            ?.name
            ?.toUpperCase(Locale.ENGLISH)
            ?.let(KycRecoveryStatus::valueOf)
            ?: KycRecoveryStatus.NONE,
        kycBlob = source.kycData?.kycData
            // Classics.
            ?.run { get("blob_id") ?: get("blobId") }
            ?.takeIf(JsonNode::isTextual)
            ?.asText()
            ?.takeIf(String::isNotEmpty),
        depositAccounts = source.externalSystemIds?.map(::DepositAccount)?.toHashSet()
            ?: mutableSetOf()
    )

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