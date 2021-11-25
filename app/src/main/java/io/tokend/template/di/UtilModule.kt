package io.tokend.template.di

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.localaccount.mnemonic.logic.EnglishMnemonicWords
import io.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import io.tokend.template.features.signin.logic.PostSignInManagerFactory
import io.tokend.template.logic.persistence.BackgroundLockManager
import io.tokend.template.util.ConnectionStateUtil
import io.tokend.template.util.cipher.Aes256GcmDataCipher
import io.tokend.template.util.cipher.DataCipher
import io.tokend.template.util.comparator.AssetCodeComparator
import io.tokend.template.util.comparator.AssetComparator
import io.tokend.template.util.comparator.BalancesByConvertedAmountComparator
import io.tokend.template.util.errorhandler.DefaultErrorLogger
import io.tokend.template.util.errorhandler.ErrorHandlerFactory
import io.tokend.template.util.errorhandler.ErrorLogger
import io.tokend.template.util.locale.AppLocaleManager
import io.tokend.template.view.ToastManager
import io.tokend.template.view.util.formatter.AmountFormatter
import io.tokend.template.view.util.formatter.DefaultAmountFormatter
import org.tokend.sdk.factory.JsonApiTools
import java.util.*
import javax.inject.Singleton

@Module
class UtilModule {
    private var errorHandlerFactory: ErrorHandlerFactory? = null
    private var errorHandlerFactoryLocale: Locale? = null

    @Provides
    fun errorHandlerFactory(
        context: Context,
        localeManager: AppLocaleManager,
        toastManager: ToastManager,
        errorLogger: ErrorLogger
    ): ErrorHandlerFactory {
        val locale = localeManager.getLocale()
        val cached = errorHandlerFactory
        return if (cached != null && errorHandlerFactoryLocale == locale)
            cached
        else
            ErrorHandlerFactory(
                localeManager.getLocalizedContext(context),
                toastManager, errorLogger
            ).also {
                errorHandlerFactory = it
                errorHandlerFactoryLocale = locale
            }
    }

    private var toastManager: ToastManager? = null
    private var toastManagerLocale: Locale? = null

    @Provides
    fun toastManager(
        context: Context,
        localeManager: AppLocaleManager
    ): ToastManager {
        val locale = localeManager.getLocale()
        val cached = toastManager
        return if (cached != null && toastManagerLocale == locale)
            cached
        else
            ToastManager(localeManager.getLocalizedContext(context)).also {
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
        return JsonApiTools.objectMapper
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

    @Provides
    @Singleton
    fun connectionStateUtil(context: Context): ConnectionStateUtil {
        return ConnectionStateUtil(context)
    }

    @Provides
    @Singleton
    fun postSignInManagerFactory(
        repositoryProvider: RepositoryProvider,
        connectionStateUtil: ConnectionStateUtil
    ): PostSignInManagerFactory {
        return PostSignInManagerFactory(repositoryProvider, connectionStateUtil)
    }
}