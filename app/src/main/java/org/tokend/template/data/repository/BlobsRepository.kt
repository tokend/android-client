package org.tokend.template.data.repository

import androidx.collection.LruCache
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.template.di.providers.ApiProvider

class BlobsRepository(
        private val apiProvider: ApiProvider
) {
    private val cache = LruCache<String, Blob>(CACHE_SIZE)

    /**
     * @param isPrivate if set to true a signed API instance will be used
     * to load the blob
     *
     * @return [Blob] loaded from the network or it's cached copy
     */
    fun getById(blobId: String,
                isPrivate: Boolean = false): Single<Blob> {
        val api =
                if (isPrivate)
                    apiProvider.getSignedApi()
                            ?: return Single.error(
                                    IllegalStateException("Cannot get signed API to load private blob")
                            )
                else
                    apiProvider.getApi()

        return cache[blobId]
                ?.toSingle()
                ?: api
                        .blobs
                        .getBlob(blobId)
                        .toSingle()
                        .doOnSuccess { blob ->
                            cache.put(blobId, blob)
                        }

    }

    companion object {
        private const val CACHE_SIZE = 20
    }
}