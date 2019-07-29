package org.tokend.template.data.model

import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl
import java.io.Serializable

class UrlConfig(
        api: String,
        storage: String,
        client: String
) : Serializable {
    @SerializedName("api")
    private val mApi: String = api

    @SerializedName("storage")
    private val mStorage: String = storage

    @SerializedName("client")
    private val mClient: String? = client

    @SerializedName("terms")
    private val mTerms: String? = null

    val api: String
        get() = mApi
                .addTrailSlashIfNeeded()
                .addProtocolIfNeeded()

    val storage: String
        get() = mStorage
                .addTrailSlashIfNeeded()
                .addProtocolIfNeeded()

    val client: String
        get() = (mClient ?: mTerms?.substringBefore(TERMS_ROUTE))
                ?.addTrailSlashIfNeeded()
                ?.addProtocolIfNeeded()
                ?: "//"

    val kyc: String
        get() = (client + KYC_ROUTE)
                .addTrailSlashIfNeeded()
                .addProtocolIfNeeded()

    val terms: String
        get() = (mTerms ?: client + TERMS_ROUTE)
                .addTrailSlashIfNeeded()
                .addProtocolIfNeeded()

    val apiDomain: String
        get() = HttpUrl.parse(api)?.host() ?: api

    private fun String.addTrailSlashIfNeeded(): String {
        return if (this.endsWith('/')) this else this + "/"
    }

    private fun String.addProtocolIfNeeded(): String {
        return if (!contains("^.+//".toRegex()))
            "https://" + this
        else if (startsWith("//"))
            "https:" + this
        else
            this
    }

    companion object {
        private const val TERMS_ROUTE = "terms"
        private const val KYC_ROUTE = "settings/verification"
    }
}