package org.tokend.template.features.kyc.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.repository.AccountRepository
import org.tokend.template.data.repository.BlobsRepository
import org.tokend.template.data.storage.persistence.ObjectPersistence
import org.tokend.template.data.storage.repository.SingleItemRepository
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.model.KycForm

/**
 * Holds user's active KYC data
 */
class ActiveKycRepository(
        private val accountRepository: AccountRepository,
        private val blobsRepository: BlobsRepository,
        persistence: ObjectPersistence<ActiveKyc>?
) : SingleItemRepository<ActiveKyc>(persistence) {
    val itemFormData: KycForm?
        get() = (item as? ActiveKyc.Form)?.formData

    override fun getItem(): Single<ActiveKyc> {
        return getAccount()
                .flatMap { account ->
                    if (account.kycBlob != null)
                        getForm(account.kycBlob).map(ActiveKyc::Form)
                    else
                        Single.just(ActiveKyc.Missing)
                }
    }

    private fun getAccount(): Single<AccountRecord> {
        return accountRepository
                .run {
                    updateDeferred()
                            .andThen(Maybe.defer { item.toMaybe() })
                            .switchIfEmpty(Single.error(IllegalStateException("Missing account")))
                }
    }

    private fun getForm(blobId: String): Single<KycForm> {
        return blobsRepository
                .getById(blobId, true)
                .map { blob ->
                    try {
                        KycForm.fromBlob(blob)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        KycForm.Empty
                    }
                }
    }
}