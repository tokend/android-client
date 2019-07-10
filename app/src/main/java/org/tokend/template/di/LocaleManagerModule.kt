package org.tokend.template.di

import dagger.Module
import dagger.Provides
import org.tokend.template.util.locale.AppLocaleManager
import javax.inject.Singleton

@Module
class LocaleManagerModule(
        private val localeManager: AppLocaleManager
) {
    @Provides
    @Singleton
    fun localeManager() = localeManager
}