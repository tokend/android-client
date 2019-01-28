package org.tokend.template.data.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle

class AccountRepository(private val apiProvider: ApiProvider,
                        private val walletInfoProvider: WalletInfoProvider)
    : SimpleSingleItemRepository<AccountRecord>() {

    override fun getItem(): Observable<AccountRecord> {
        return getAccountResponse().toObservable()
    }

    private fun getAccountResponse(): Single<AccountRecord> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .accounts
                .getById(accountId)
                .toSingle()
                .map {
                    AccountRecord(it)
                }
    }
}