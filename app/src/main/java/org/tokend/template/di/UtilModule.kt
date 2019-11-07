package org.tokend.template.di

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.features.localaccount.mnemonic.logic.EnglishMnemonicWords
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.logic.persistence.BackgroundLockManager
import org.tokend.template.util.cipher.Aes256GcmDataCipher
import org.tokend.template.util.cipher.DataCipher
import org.tokend.template.util.comparator.AssetCodeComparator
import org.tokend.template.util.comparator.AssetComparator
import org.tokend.template.util.comparator.BalancesByConvertedAmountComparator
import org.tokend.template.util.errorhandler.DefaultErrorLogger
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.util.errorhandler.ErrorLogger
import org.tokend.template.util.locale.AppLocaleManager
import org.tokend.template.view.ToastManager
import org.tokend.template.view.util.formatter.AmountFormatter
import org.tokend.template.view.util.formatter.DefaultAmountFormatter
import java.util.*
import javax.inject.Singleton

@Module
class UtilModule {
    private var errorHandlerFactory: ErrorHandlerFactory? = null
    private var errorHandlerFactoryLocale: Locale? = null

    @Provides
    fun errorHandlerFactory(context: Context,
                            localeManager: AppLocaleManager,
                            toastManager: ToastManager,
                            errorLogger: ErrorLogger): ErrorHandlerFactory {
        val locale = localeManager.getLocale()
        val cached = errorHandlerFactory
        return if (cached != null && errorHandlerFactoryLocale == locale)
            cached
        else
            ErrorHandlerFactory(localeManager.getLocalizeContext(context),
                    toastManager, errorLogger).also {
                errorHandlerFactory = it
                errorHandlerFactoryLocale = locale
            }
    }

    private var toastManager: ToastManager? = null
    private var toastManagerLocale: Locale? = null

    @Provides
    fun toastManager(context: Context,
                     localeManager: AppLocaleManager): ToastManager {
        val locale = localeManager.getLocale()
        val cached = toastManager
        return if (cached != null && toastManagerLocale == locale)
            cached
        else
            ToastManager(localeManager.getLocalizeContext(context)).also {
                toastManager = it
                toastManagerLocale = locale
            }
    }

    @Provides
    @Singleton
    fun assetCodeComparator(): Comparator<String> {
        return AssetCodeComparator()
    }

    @Provides
    @Singleton
    fun assetComparator(): Comparator<Asset> {
        return AssetComparator(assetCodeComparator())
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

    @Provides
    @Singleton
    fun errorLogger(): ErrorLogger {
        return DefaultErrorLogger()
    }

    @Provides
    @Singleton
    fun backgroundLockManager(context: Context): BackgroundLockManager {
        return BackgroundLockManager(context)
    }

    @Provides
    @Singleton
    fun mnemonicCode(): MnemonicCode {
        return MnemonicCode(EnglishMnemonicWords.LIST)
    }

    @Provides
    @Singleton
    fun dataCipher(): DataCipher {
        return Aes256GcmDataCipher()
    }
}