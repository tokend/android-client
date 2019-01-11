package org.tokend.template.di.providers

import android.content.Context
import org.tokend.template.data.repository.*
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.favorites.FavoritesRepository
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.data.repository.orderbook.OrderBookRepository
import org.tokend.template.data.repository.pairs.AssetPairsRepository
import org.tokend.template.data.repository.tfa.TfaFactorsRepository
import org.tokend.template.data.repository.transactions.TxRepository
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.send.repository.ContactsRepository

/**
 * @param context if not specified then android-related repositories
 * will be unavailable
 */
class RepositoryProviderImpl(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val urlConfigProvider: UrlConfigProvider,
        private val context: Context? = null
) : RepositoryProvider {
    private val balancesRepository: BalancesRepository by lazy {
        BalancesRepository(apiProvider, walletInfoProvider)
    }
    private val transactionsRepositoriesByAsset = mutableMapOf<String, TxRepository>()
    private val accountDetails: AccountDetailsRepository by lazy {
        AccountDetailsRepository(apiProvider)
    }
    private val systemInfoRepository: SystemInfoRepository by lazy {
        SystemInfoRepository(apiProvider)
    }
    private val tfaFactorsRepository: TfaFactorsRepository by lazy {
        TfaFactorsRepository(apiProvider, walletInfoProvider)
    }
    private val assetsRepository: AssetsRepository by lazy {
        AssetsRepository(apiProvider)
    }
    private val orderBookRepositories = mutableMapOf<String, OrderBookRepository>()
    private val assetPairsRepository: AssetPairsRepository by lazy {
        AssetPairsRepository(apiProvider)
    }
    private val offersRepositories = mutableMapOf<String, OffersRepository>()
    private val accountRepository: AccountRepository by lazy {
        AccountRepository(apiProvider, walletInfoProvider)
    }
    private val userRepository: UserRepository by lazy {
        UserRepository(apiProvider, walletInfoProvider)
    }
    private val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepository(apiProvider, walletInfoProvider)
    }
    private val salesRepository: SalesRepository by lazy {
        SalesRepository(apiProvider, urlConfigProvider)
    }

    private val filteredSalesRepository: SalesRepository by lazy {
        SalesRepository(apiProvider, urlConfigProvider)
    }

    private val contactsRepository: ContactsRepository by lazy {
        context ?: throw IllegalStateException("This provider has no context " +
                "required to provide contacts repository")
        ContactsRepository(context)
    }

    private val limitsRepository: LimitsRepository by lazy {
        LimitsRepository(apiProvider, walletInfoProvider)
    }

    private val feesRepository: FeesRepository by lazy {
        FeesRepository(apiProvider, walletInfoProvider)
    }

    override fun balances(): BalancesRepository {
        return balancesRepository
    }

    override fun transactions(asset: String): TxRepository {
        return transactionsRepositoriesByAsset.getOrPut(asset) {
            TxRepository(apiProvider, walletInfoProvider, asset, accountDetails())
        }
    }

    override fun accountDetails(): AccountDetailsRepository {
        return accountDetails
    }

    override fun systemInfo(): SystemInfoRepository {
        return systemInfoRepository
    }

    override fun tfaFactors(): TfaFactorsRepository {
        return tfaFactorsRepository
    }

    override fun assets(): AssetsRepository {
        return assetsRepository
    }

    override fun assetPairs(): AssetPairsRepository {
        return assetPairsRepository
    }

    override fun orderBook(baseAsset: String,
                           quoteAsset: String,
                           isBuy: Boolean): OrderBookRepository {
        val key = "$baseAsset.$quoteAsset.$isBuy"
        return orderBookRepositories.getOrPut(key) {
            OrderBookRepository(apiProvider, baseAsset, quoteAsset, isBuy)
        }
    }

    override fun offers(onlyPrimaryMarket: Boolean): OffersRepository {
        val key = "$onlyPrimaryMarket"
        return offersRepositories.getOrPut(key) {
            OffersRepository(apiProvider, walletInfoProvider, onlyPrimaryMarket)
        }
    }

    override fun account(): AccountRepository {
        return accountRepository
    }

    override fun user(): UserRepository {
        return userRepository
    }

    override fun favorites(): FavoritesRepository {
        return favoritesRepository
    }

    override fun sales(): SalesRepository {
        return salesRepository
    }

    override fun filteredSales(): SalesRepository {
        return filteredSalesRepository
    }

    override fun contacts(): ContactsRepository {
        return contactsRepository
    }

    override fun limits(): LimitsRepository {
        return limitsRepository
    }

    override fun fees(): FeesRepository {
        return feesRepository
    }
}