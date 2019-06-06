package org.tokend.template.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.generated.resources.AssetResource
import org.tokend.sdk.utils.HashCodes
import org.tokend.template.util.PolicyChecker
import java.io.Serializable
import java.math.BigDecimal

class AssetRecord(
        override val code: String,
        val policy: Int,
        override val name: String?,
        val logoUrl: String?,
        val terms: RemoteFile?,
        val externalSystemType: Int?,
        val issued: BigDecimal?,
        val available: BigDecimal?,
        val maximum: BigDecimal,
        override val trailingDigits: Int
) : Serializable, PolicyChecker, Asset {
    val isBackedByExternalSystem: Boolean
        get() = externalSystemType != null

    val isTransferable: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.TRANSFERABLE.value)

    val isWithdrawable: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.WITHDRAWABLE.value)

    override fun equals(other: Any?): Boolean {
        return other is AssetRecord
                && other.code == this.code
                && other.logoUrl == this.logoUrl
    }

    override fun hashCode(): Int {
        return HashCodes.ofMany(code, logoUrl)
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

            return AssetRecord(
                    code = source.id,
                    policy = source.policies.value,
                    name = name,
                    logoUrl = logo?.getUrl(urlConfig?.storage),
                    terms = terms,
                    externalSystemType = externalSystemType,
                    issued = source.issued,
                    available = source.availableForIssuance,
                    maximum = source.maxIssuanceAmount,
                    trailingDigits = source.trailingDigits.toInt()
            )
        }
    }
}