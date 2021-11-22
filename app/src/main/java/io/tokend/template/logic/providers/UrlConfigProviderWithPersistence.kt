package io.tokend.template.logic.providers

import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.features.urlconfig.model.UrlConfig

class UrlConfigProviderWithPersistence(
    private val defaultConfig: UrlConfig,
    private val persistence: ObjectPersistence<UrlConfig>
) : UrlConfigProvider {
    private var mUrlConfig: UrlConfig? = null

    override fun getConfig(): UrlConfig {
        return mUrlConfig
            ?: (persistence.loadItem() ?: defaultConfig).also { mUrlConfig = it }
    }

    override fun setConfig(config: UrlConfig) {
        mUrlConfig = config
        persistence.saveItem(config)
    }
}