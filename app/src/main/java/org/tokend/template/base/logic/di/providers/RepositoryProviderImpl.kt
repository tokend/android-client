package org.tokend.template.base.logic.di.providers

import android.content.Context
import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.AccountRepository
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.UserRepository
import org.tokend.template.base.logic.repository.assets.AssetsRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.features.send.repository.ContactsRepository
import org.tokend.template.base.logic.repository.favorites.FavoritesRepository
import org.tokend.template.base.logic.repository.pairs.AssetPairsRepository
import org.tokend.template.base.logic.repository.tfa.TfaBackendsRepository
import org.tokend.template.base.logic.repository.transactions.TxRepository
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.trade.repository.offers.OffersRepository
import org.tokend.template.features.trade.repository.order_book.OrderBookRepository

class RepositoryProviderImpl(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val context: Context
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
    private val tfaBackendsRepository: TfaBackendsRepository by lazy {
        TfaBackendsRepository(apiProvider, walletInfoProvider)
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
        SalesRepository(apiProvider, accountDetails())
    }

    private val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(context)
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

    override fun tfaBackends(): TfaBackendsRepository {
        return tfaBackendsRepository
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

    override fun contacts(): ContactsRepository {
        return contactsRepository
    }
}