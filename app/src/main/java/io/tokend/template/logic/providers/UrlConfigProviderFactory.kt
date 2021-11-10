package io.tokend.template.logic.providers

import io.tokend.template.features.urlconfig.model.UrlConfig

class UrlConfigProviderFactory {
    fun createUrlConfigProvider(): UrlConfigProvider {
        return UrlConfigProviderImpl()
    }

    fun createUrlConfigProvider(config: UrlConfig): UrlConfigProvider {
        return createUrlConfigProvider().apply { setConfig(config) }
    }
}