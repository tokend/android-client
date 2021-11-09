package io.tokend.template.di

import dagger.Module
import dagger.Provides
import io.tokend.template.features.tfa.logic.AppTfaCallback
import javax.inject.Singleton

@Module
class AppTfaCallbackModule {
    @Provides
    @Singleton
    fun provideAppTfaCallback(): AppTfaCallback {
        return AppTfaCallback()
    }
}