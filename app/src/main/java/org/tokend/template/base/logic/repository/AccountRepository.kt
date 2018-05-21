package org.tokend.template.base.logic.repository

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.sdk.api.responses.AccountResponse
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.base.SimpleSingleItemRepository
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.Account

class AccountRepository(private val apiProvider: ApiProvider,
                        private val walletInfoProvider: WalletInfoProvider)
    : SimpleSingleItemRepository<AccountResponse>() {

    override fun getItem(): Observable<AccountResponse> {
        return getAccountResponse().toObservable()
    }

    private fun getAccountResponse() : Single<AccountResponse> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.getAccount(accountId).toSingle()
    }
}