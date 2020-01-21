package org.tokend.template.data.repository

import io.reactivex.Maybe
import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.wallet.NetworkParams

class SystemInfoRepository(
        private val apiProvider: ApiProvider
) : SingleItemRepository<SystemInfo>() {
    override fun getItem(): Maybe<SystemInfo> {
        return apiProvider.getApi()
                .general
                .getSystemInfo()
                .toSingle()
                .toMaybe()
    }

    fun getNetworkParams(): Single<NetworkParams> {
        return updateIfNotFreshDeferred()
                .toSingle {
                    item?.toNetworkParams()
                            ?: throw IllegalStateException("Missing network passphrase")
                }
    }
}