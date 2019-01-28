package org.tokend.template.features.invest.logic

import io.reactivex.Single
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle

class BlobManager(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) {
    fun getBlob(blobId: String): Single<Blob> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        return apiProvider.getApi()
                .blobs
                .getById(
                        accountId,
                        blobId
                )
                .toSingle()
    }
}