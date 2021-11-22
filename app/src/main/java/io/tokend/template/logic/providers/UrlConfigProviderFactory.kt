package io.tokend.template.logic.providers

import io.tokend.template.data.storage.persistence.MemoryOnlyObjectPersistence
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.features.urlconfig.model.UrlConfig

class UrlConfigProviderFactory {
    fun createUrlConfigProvider(
        defaultConfig: UrlConfig,
        persistence: ObjectPersistence<UrlConfig> = MemoryOnlyObjectPersistence<UrlConfig>()
    ): UrlConfigProvider {
        return UrlConfigProviderWithPersistence(
            defaultConfig,
            persistence
        )
    }
}