package io.tokend.template.di.providers

import android.content.Context
import dagger.Module
import dagger.Provides
import io.tokend.template.App
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {

    @Provides
    @Singleton
    fun appContext(): Context = app.applicationContext
}