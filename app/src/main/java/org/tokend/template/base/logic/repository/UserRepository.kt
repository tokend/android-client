package org.tokend.template.base.logic.repository

import io.reactivex.Completable
import io.reactivex.Observable
import org.tokend.sdk.api.requests.AttributesEntity
import org.tokend.sdk.api.requests.DataEntity
import org.tokend.sdk.api.requests.models.CreateUserRequestBody
import org.tokend.sdk.api.responses.UserInfo
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.base.SimpleSingleItemRepository
import org.tokend.template.extensions.toCompletable
import org.tokend.template.extensions.toSingle

class UserRepository(private val apiProvider: ApiProvider,
                     private val walletInfoProvider: WalletInfoProvider
) : SimpleSingleItemRepository<UserInfo>() {

    override fun getItem(): Observable<UserInfo> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Observable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Observable.error(IllegalStateException("No wallet info found"))

        return signedApi.getUserInfo(accountId)
                .toSingle()
                .map { it.data!! }
                .toObservable()
    }

    fun createUnverified(): Completable {
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))

        return signedApi.createUser(accountId,
                DataEntity(
                        AttributesEntity(
                                CreateUserRequestBody(DEFAULT_USER_TYPE)
                        )
                )
        )
                .toCompletable()
    }

    companion object {
        const val DEFAULT_USER_TYPE = "not_verified"
    }
}