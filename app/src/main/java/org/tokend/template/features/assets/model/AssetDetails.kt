package org.tokend.template.features.assets.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.utils.HashCodes
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
open class AssetDetails(
        @JsonProperty("name")
        val name: String,

        @JsonProperty("logo")
        open val logo: RemoteFile?,

        @JsonProperty("terms")
        open val terms: RemoteFile?,

        @JsonProperty("external_system_type")
        protected open val externalSystemTypeString: String?
) : Serializable {
    open val externalSystemType: Int?
        get() = externalSystemTypeString?.toIntOrNull()

    override fun equals(other: Any?): Boolean {
        return other is AssetDetails
                && other.name == this.name
                && other.terms == this.terms
                && other.logo == logo
                && other.externalSystemType == other.externalSystemType
    }

    override fun hashCode(): Int {
        return HashCodes.ofMany(name, terms, logo, externalSystemType)
    }
}