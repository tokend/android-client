package io.tokend.template.data.repository

import androidx.collection.LruCache
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.blobs.model.Blob

class BlobsRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider
) {
    private val cache = LruCache<String, Blob>(CACHE_SIZE)

    /**
     * @param isPrivate if set to true a signed API instance will be used
     * to load the blob
     *
     * @return [Blob] loaded from the network or it's cached copy
     */
    fun getById(
        blobId: String,
        isPrivate: Boolean = false
    ): Single<Blob> {
        val api =
            if (isPrivate)
                apiProvider.getSignedApi()
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

    fun create(blob: Blob): Single<Blob> {
        val signedApi = apiProvider.getSignedApi()
        val accountId = walletInfoProvider.getWalletInfo().accountId

        return signedApi
            .blobs
            .create(blob, accountId)
            .toSingle()
            .doOnSuccess { cache.put(it.id, it) }
    }

    companion object {
        private const val CACHE_SIZE = 20
    }
}