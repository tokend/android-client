package io.tokend.template.features.kyc.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.data.model.AccountRecord
import io.tokend.template.data.repository.AccountRepository
import io.tokend.template.data.repository.BlobsRepository
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.model.KycForm

/**
 * Holds user's active KYC data
 */
class ActiveKycRepository(
    private val accountRepository: AccountRepository,
    private val blobsRepository: BlobsRepository,
    private val keyValueEntriesRepository: KeyValueEntriesRepository,
    persistence: ObjectPersistence<ActiveKyc>?
) : SingleItemRepository<ActiveKyc>(persistence) {
    val itemFormData: KycForm?
        get() = (item as? ActiveKyc.Form)?.formData

    override fun getItem(): Single<ActiveKyc> {
        return getAccount()
            .flatMap { account ->
                if (account.kycBlob != null)
                    getForm(account.kycBlob, account.roleId).map(ActiveKyc::Form)
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

    private fun getForm(blobId: String, roleId: Long): Single<KycForm> {
        return blobsRepository
            .getById(blobId, true)
            .flatMap { blob ->
                keyValueEntriesRepository
                    .updateIfNotFreshDeferred()
                    .toSingle {
                        blob to keyValueEntriesRepository.itemsList
                    }
            }
            .map { (blob, keyValueEntries) ->
                KycForm.fromBlob(blob, roleId, keyValueEntries)
            }
    }
}