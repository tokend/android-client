package org.tokend.template.base.logic.di

import dagger.Module
import dagger.Provides
import org.tokend.template.base.logic.AppTfaCallback
import javax.inject.Singleton

@Module
class AppTfaCallbackModule {
    @Provides
    @Singleton
    fun provideAppTfaCallback(): AppTfaCallback {
        return AppTfaCallback()
    }
}