package org.tokend.template.base.logic.di.providers

import org.tokend.template.base.logic.model.UrlConfig

class UrlConfigProviderFactory {
    fun createUrlConfigProvider(): UrlConfigProvider {
        return UrlConfigProviderImpl()
    }

    fun createUrlConfigProvider(config: UrlConfig): UrlConfigProvider {
        return createUrlConfigProvider().apply { setConfig(config) }
    }
}