package org.tokend.template.di

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.util.comparator.AssetComparator
import org.tokend.template.util.comparator.BalancesByConvertedAmountComparator
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.view.ToastManager
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DefaultAmountFormatter
import javax.inject.Singleton

@Module
class UtilModule {
    @Provides
    @Singleton
    fun errorHandlerFactory(context: Context, toastManager: ToastManager): ErrorHandlerFactory {
        return ErrorHandlerFactory(context, toastManager)
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
    fun balanceComparator(): Comparator<BalanceRecord> {
        return BalancesByConvertedAmountComparator(assetComparator())
    }

    @Provides
    @Singleton
    fun amountFormatter(): AmountFormatter {
        return DefaultAmountFormatter()
    }

    @Provides
    @Singleton
    fun objectMapper(): ObjectMapper {
        return JsonApiToolsProvider.getObjectMapper()
    }
}