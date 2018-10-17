package org.tokend.template.base.logic.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.base.SimpleSingleItemRepository
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.NetworkParams

class SystemInfoRepository(
        private val apiProvider: ApiProvider
) : SimpleSingleItemRepository<SystemInfo>() {
    override fun getItem(): Observable<SystemInfo> {
        return apiProvider.getApi().general.getSystemInfo().toSingle().toObservable()
    }

    fun getNetworkParams(): Single<NetworkParams> {
        return updateIfNotFreshDeferred()
                .toSingle {
                    item?.toNetworkParams()
                            ?: throw IllegalStateException("Missing network passphrase")
                }
    }
}