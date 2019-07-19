package org.tokend.template.features.invest.logic

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.template.di.providers.ApiProvider

class BlobManager(
        private val apiProvider: ApiProvider
) {
    fun getBlob(blobId: String): Single<Blob> {
        return apiProvider.getApi()
                .blobs
                .getBlob(blobId)
                .toSingle()
    }

    fun getPrivateBlob(blobId: String): Single<Blob> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .blobs
                .getBlob(blobId)
                .toSingle()
    }
}