package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.features.urlconfig.model.UrlConfig
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.logic.providers.UrlConfigProviderFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class UrlConfigModule(
    private val defaultConfig: UrlConfig
) {
    @Provides
    @Singleton
    fun urlConfigProvider(
        @Named("url_config")
        persistence: ObjectPersistence<UrlConfig>
    ): UrlConfigProvider {
        return UrlConfigProviderFactory().createUrlConfigProvider(
            this.defaultConfig,
            persistence
        )
    }
}