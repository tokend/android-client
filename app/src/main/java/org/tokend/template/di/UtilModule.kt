package org.tokend.template.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tokend.template.view.ToastManager
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
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