package org.tokend.template.data.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.sdk.api.accounts.model.Account
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.extensions.toSingle

class AccountRepository(private val apiProvider: ApiProvider,
                        private val walletInfoProvider: WalletInfoProvider)
    : SimpleSingleItemRepository<Account>() {

    override fun getItem(): Observable<Account> {
        return getAccountResponse().toObservable()
    }

    private fun getAccountResponse(): Single<Account> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .accounts
                .getById(accountId)
                .toSingle()
    }
}