package org.tokend.template.features.systeminfo.storage

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.template.data.repository.base.ObjectPersistence
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.features.systeminfo.model.SystemInfoRecord
import org.tokend.wallet.NetworkParams

class SystemInfoRepository(
        private val apiProvider: ApiProvider,
        persistence: ObjectPersistence<SystemInfoRecord>
) : SingleItemRepository<SystemInfoRecord>(persistence) {
    override fun getItem(): Single<SystemInfoRecord> {
        return apiProvider.getApi()
                .general
                .getSystemInfo()
                .toSingle()
                .map(::SystemInfoRecord)
    }

    fun getNetworkParams(): Single<NetworkParams> {
        return ensureData()
                .toSingle {
                    item?.toNetworkParams()
                            ?: throw IllegalStateException("Missing network passphrase")
                }
    }
}