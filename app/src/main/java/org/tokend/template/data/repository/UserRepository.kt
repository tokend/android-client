package org.tokend.template.data.repository

import io.reactivex.Completable
import io.reactivex.Observable
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.User
import org.tokend.template.extensions.toCompletable
import org.tokend.template.extensions.toSingle

class UserRepository(private val apiProvider: ApiProvider,
                     private val walletInfoProvider: WalletInfoProvider
) : SimpleSingleItemRepository<User>() {

    override fun getItem(): Observable<User> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Observable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Observable.error(IllegalStateException("No wallet info found"))

        return signedApi.users.get(accountId)
                .toSingle()
                .toObservable()
    }

    /**
     * Creates default user which also triggers account creation
     */
    fun createUnverified(): Completable {
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))

        return signedApi.users.create(accountId, DEFAULT_USER_TYPE).toCompletable()
    }

    companion object {
        const val DEFAULT_USER_TYPE = "not_verified"
    }
}