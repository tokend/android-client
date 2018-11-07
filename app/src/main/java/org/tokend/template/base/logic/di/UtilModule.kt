package org.tokend.template.base.logic.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import javax.inject.Singleton

@Module
class UtilModule {
    @Provides
    @Singleton
    fun errorHandlerFactory(context: Context): ErrorHandlerFactory{
        return ErrorHandlerFactory(context)
    }

    @Provides
    @Singleton
    fun toastManager(context: Context): ToastManager {
        return ToastManager(context)
    }
}