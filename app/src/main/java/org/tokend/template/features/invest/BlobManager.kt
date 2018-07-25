package org.tokend.template.features.invest

import io.reactivex.Single
import org.tokend.sdk.api.models.Blob
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.extensions.toSingle

class BlobManager(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) {
    fun getBlob(blobId: String): Single<Blob> {
        return apiProvider.getApi()
                .getBlob(walletInfoProvider.getWalletInfo()?.accountId, blobId)
                .toSingle()
                .map { it.data!! }
    }
}