package org.tokend.template.features.assets.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.sdk.api.assets.model.SimpleAsset
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.generated.resources.AssetResource
import org.tokend.template.data.model.UrlConfig
import org.tokend.template.util.PolicyChecker
import java.io.Serializable
import java.math.BigDecimal

class AssetRecord(
        val code: String,
        val policy: Int,
        val name: String?,
        val logoUrl: String?,
        val terms: RemoteFile?,
        val externalSystemType: Int?,
        val issued: BigDecimal?,
        val available: BigDecimal?,
        val maximum: BigDecimal
) : Serializable, PolicyChecker {

    constructor(source: SimpleAsset,
                urlConfig: UrlConfig?
    ) : this(
            code = source.code,
            policy = source.policy,
            name = source.details.name,
            logoUrl = source.details.logo?.getUrl(urlConfig?.storage),
            terms = source.details.terms,
            externalSystemType = source.details.externalSystemType,
            issued = source.issued,
            available = source.available,
            maximum = source.maximum
    )

    val isBackedByExternalSystem: Boolean
        get() = externalSystemType != null

    val isTransferable: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.TRANSFERABLE.value)

    val isWithdrawable: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.WITHDRAWABLE.value)

    override fun equals(other: Any?): Boolean {
        return other is AssetRecord
                && other.code == this.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromResource(source: AssetResource, urlConfig: UrlConfig?, mapper: ObjectMapper): AssetRecord {
            val details = mapper.convertValue(source.details, AssetDetails::class.java)

            return AssetRecord(
                    code = source.id,
                    policy = source.policies.value,
                    name = details.name,
                    logoUrl = details.logo?.getUrl(urlConfig?.storage),
                    terms = details.terms,
                    externalSystemType = details.externalSystemType,
                    issued = source.issued,
                    available = source.availableForIssuance,
                    maximum = source.maxIssuanceAmount
            )
        }
    }
}