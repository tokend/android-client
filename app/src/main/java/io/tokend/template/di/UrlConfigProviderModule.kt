package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.logic.providers.UrlConfigProviderFactory
import io.tokend.template.features.urlconfig.model.UrlConfig
import javax.inject.Singleton

@Module
class UrlConfigProviderModule(
    private val defaultConfig: UrlConfig?
) {
    @Provides
    @Singleton
    fun urlConfigProvider(): UrlConfigProvider {
        return UrlConfigProviderFactory().createUrlConfigProvider()
            .also {
                if (defaultConfig != null) {
                    it.setConfig(defaultConfig)
                }
            }
    }
}