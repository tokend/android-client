package org.tokend.template.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tokend.template.util.AssetComparator
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.view.ToastManager
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DefaultAmountFormatter
import javax.inject.Singleton

@Module
class UtilModule {
    @Provides
    @Singleton
    fun errorHandlerFactory(context: Context): ErrorHandlerFactory {
        return ErrorHandlerFactory(context)
    }

    @Provides
    @Singleton
    fun toastManager(context: Context): ToastManager {
        return ToastManager(context)
    }

    @Provides
    @Singleton
    fun assetComparator(): Comparator<String> {
        return AssetComparator()
    }

    @Provides
    @Singleton
    fun amountFormatter(): AmountFormatter {
        return DefaultAmountFormatter()
    }
}