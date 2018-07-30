package org.tokend.template.base.logic.model

import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl
import java.io.Serializable

data class UrlConfig(
        @SerializedName("api")
        private val mApi: String,
        @SerializedName("storage")
        private val mStorage: String,
        @SerializedName("kyc")
        private val mKyc: String,
        @SerializedName("terms")
        private val mTerms: String
) : Serializable {
    val api: String
        get() = mApi.addTrailSlashIfNeeded()

    val storage: String
        get() = mStorage.addTrailSlashIfNeeded()

    val kyc: String
        get() = mKyc.addTrailSlashIfNeeded()

    val terms: String
        get() = mTerms.addTrailSlashIfNeeded()

    val apiDomain: String
        get() = HttpUrl.parse(api)?.host() ?: api

    private fun String.addTrailSlashIfNeeded(): String {
        return if (this.endsWith('/')) this else this + "/"
    }
}