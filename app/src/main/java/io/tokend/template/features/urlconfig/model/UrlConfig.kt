package io.tokend.template.features.urlconfig.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import okhttp3.HttpUrl
import java.io.Serializable

class UrlConfig
@JsonCreator
constructor(
    api: String,
    storage: String,
    client: String
) : Serializable {
    private val mApi: String = api
    private val mStorage: String = storage
    private val mClient: String = client

    @get:JsonProperty("api")
    val api: String
        get() = mApi
            .addTrailSlashIfNeeded()
            .addProtocolIfNeeded()

    @get:JsonProperty("storage")
    val storage: String
        get() = mStorage
            .addTrailSlashIfNeeded()
            .addProtocolIfNeeded()

    @get:JsonProperty("client")
    val client: String
        get() = mClient
            .addTrailSlashIfNeeded()
            .addProtocolIfNeeded()

    @get:JsonIgnore
    val kyc: String
        get() = (client + KYC_ROUTE)
            .addTrailSlashIfNeeded()
            .addProtocolIfNeeded()

    @get:JsonIgnore
    val terms: String
        get() = (client + TERMS_ROUTE)
            .addTrailSlashIfNeeded()
            .addProtocolIfNeeded()

    @get:JsonIgnore
    val apiDomain: String
        get() = HttpUrl.parse(api)?.host() ?: api

    private fun String.addTrailSlashIfNeeded(): String {
        return if (this.endsWith('/')) this else "$this/"
    }

    private fun String.addProtocolIfNeeded(): String {
        return if (!contains("^.+//".toRegex()))
            "https://$this"
        else if (startsWith("//"))
            "https:$this"
        else
            this
    }

    companion object {
        private const val TERMS_ROUTE = "terms"
        private const val KYC_ROUTE = "settings/verification"

        fun fromQrJson(qrJson: JsonNode) = UrlConfig(
            api = qrJson
                .get("api")
                ?.takeIf(JsonNode::isTextual)
                ?.asText()
                ?.takeIf(String::isNotEmpty)
                ?: throw IllegalArgumentException("Invalid API URL: $qrJson"),
            storage = qrJson
                .get("storage")
                ?.takeIf(JsonNode::isTextual)
                ?.asText()
                ?.takeIf(String::isNotEmpty)
                ?: throw IllegalArgumentException("Invalid storage URL: $qrJson"),
            client = qrJson
                .get("client")
                ?.takeIf(JsonNode::isTextual)
                ?.asText()
                ?.takeIf(String::isNotEmpty)
                ?: (qrJson
                    .get("terms")
                    ?.takeIf(JsonNode::isTextual)
                    ?.asText()
                    ?.takeIf(String::isNotEmpty)
                    ?.substringBefore(TERMS_ROUTE)
                    ?: throw IllegalArgumentException("Invalid terms URL: $qrJson"))
        )
    }
}