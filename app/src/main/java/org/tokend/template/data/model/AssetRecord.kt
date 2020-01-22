package org.tokend.template.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.generated.resources.AssetResource
import org.tokend.sdk.api.v3.assets.model.AssetState
import org.tokend.template.util.RecordWithPolicy
import java.io.Serializable
import java.math.BigDecimal

class AssetRecord(
        override val code: String,
        override val policy: Int,
        override val name: String?,
        override val logoUrl: String?,
        override val description: String?,
        val terms: RemoteFile?,
        val externalSystemType: Int?,
        val issued: BigDecimal,
        val available: BigDecimal,
        val maximum: BigDecimal,
        val ownerAccountId: String,
        override val trailingDigits: Int,
        val state: AssetState
) : Serializable, RecordWithPolicy, Asset, RecordWithLogo, RecordWithDescription {
    val isBackedByExternalSystem: Boolean
        get() = externalSystemType != null

    val isTransferable: Boolean
        get() = hasPolicy(org.tokend.wallet.xdr.AssetPolicy.TRANSFERABLE.value)

    val isWithdrawable: Boolean
        get() = hasPolicy(org.tokend.wallet.xdr.AssetPolicy.WITHDRAWABLE.value)

    val canBeBaseForAtomicSwap: Boolean
        get() = hasPolicy(org.tokend.wallet.xdr.AssetPolicy.CAN_BE_BASE_IN_ATOMIC_SWAP.value)

    val isActive: Boolean
        get() = state == AssetState.ACTIVE

    override fun equals(other: Any?): Boolean {
        return other is AssetRecord && other.code == this.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun toString(): String {
        return code
    }

    companion object {
        @JvmStatic
        fun fromResource(source: AssetResource, urlConfig: UrlConfig?, mapper: ObjectMapper): AssetRecord {

            val name = source.details.get("name")?.takeIf { it !is NullNode }?.asText()

            val logo = source.details.get("logo")?.takeIf { it !is NullNode }?.let {
                mapper.convertValue(it, RemoteFile::class.java)
            }

            val terms = source.details.get("terms")?.takeIf { it !is NullNode }?.let {
                mapper.convertValue(it, RemoteFile::class.java)
            }

            val externalSystemType =
                    source.details.get("external_system_type")
                            ?.asText("")
                            ?.takeIf { it.isNotEmpty() }
                            ?.toIntOrNull()

            val description = source.details.get("description")
                    ?.asText("")
                    ?.takeIf(String::isNotEmpty)

            return AssetRecord(
                    code = source.id,
                    policy = source.policies.value
                            ?: throw IllegalStateException("Asset must have a policy"),
                    name = name,
                    logoUrl = logo?.getUrl(urlConfig?.storage),
                    terms = terms,
                    externalSystemType = externalSystemType,
                    issued = source.issued,
                    available = source.availableForIssuance,
                    maximum = source.maxIssuanceAmount,
                    ownerAccountId = source.owner.id,
                    trailingDigits = source.trailingDigits.toInt(),
                    description = description,
                    state = AssetState.fromValue(source.state.value)
            )
        }
    }
}