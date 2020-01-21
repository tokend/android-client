package org.tokend.template.data.repository

import io.reactivex.Maybe
import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.template.data.model.SystemInfoRecord
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.wallet.NetworkParams

class SystemInfoRepository(
        private val apiProvider: ApiProvider,
        persistence: ObjectPersistence<SystemInfoRecord>
) : SingleItemRepository<SystemInfoRecord>(persistence) {
    override fun getItem(): Maybe<SystemInfoRecord> {
        return apiProvider.getApi()
                .general
                .getSystemInfo()
                .toSingle()
                .map(::SystemInfoRecord)
                .toMaybe()
    }

    fun getNetworkParams(): Single<NetworkParams> {
        return ensureData()
                .toSingle {
                    item?.toNetworkParams()
                            ?: throw IllegalStateException("Missing network passphrase")
                }
    }
}