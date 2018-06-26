package org.tokend.template.base.logic.di.providers

import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.AccountRepository
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.UserRepository
import org.tokend.template.base.logic.repository.assets.AssetsRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.repository.favorites.FavoritesRepository
import org.tokend.template.base.logic.repository.pairs.AssetPairsRepository
import org.tokend.template.base.logic.repository.tfa.TfaBackendsRepository
import org.tokend.template.base.logic.repository.transactions.TxRepository
import org.tokend.template.features.trade.repository.offers.OffersRepository
import org.tokend.template.features.trade.repository.order_book.OrderBookRepository

interface RepositoryProvider {
    fun balances(): BalancesRepository
    fun transactions(asset: String): TxRepository
    fun accountDetails(): AccountDetailsRepository
    fun systemInfo(): SystemInfoRepository
    fun tfaBackends(): TfaBackendsRepository
    fun assets(): AssetsRepository
    fun assetPairs(): AssetPairsRepository
    fun orderBook(baseAsset: String, quoteAsset: String, isBuy: Boolean): OrderBookRepository
    fun offers(onlyPrimaryMarket: Boolean = true): OffersRepository
    fun account(): AccountRepository
    fun user(): UserRepository
    fun favorites(): FavoritesRepository
}