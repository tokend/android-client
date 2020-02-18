package org.tokend.template.di

import dagger.Module
import dagger.Provides
import org.tokend.template.features.tfa.logic.AppTfaCallback
import javax.inject.Singleton

@Module
class AppTfaCallbackModule {
    @Provides
    @Singleton
    fun provideAppTfaCallback(): AppTfaCallback {
        return AppTfaCallback()
    }
}