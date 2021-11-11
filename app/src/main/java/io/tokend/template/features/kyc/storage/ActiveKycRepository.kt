package io.tokend.template.features.kyc.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.data.repository.BlobsRepository
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm

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
                    getForm(account.kycBlob, account.role.role).map(ActiveKyc::Form)
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

    private fun getForm(blobId: String, accountRole: AccountRole): Single<KycForm> {
        return blobsRepository
            .getById(blobId, true)
            .map { blob ->
                KycForm.fromBlob(blob, accountRole)
            }
    }
}