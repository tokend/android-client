package org.tokend.template.di.providers

import org.tokend.template.data.repository.*
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.send.repository.ContactsRepository
import org.tokend.template.data.repository.favorites.FavoritesRepository
import org.tokend.template.data.repository.pairs.AssetPairsRepository
import org.tokend.template.data.repository.tfa.TfaFactorsRepository
import org.tokend.template.data.repository.transactions.TxRepository
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.data.repository.orderbook.OrderBookRepository

interface RepositoryProvider {
    fun balances(): BalancesRepository
    fun transactions(asset: String): TxRepository
    fun accountDetails(): AccountDetailsRepository
    fun systemInfo(): SystemInfoRepository
    fun tfaFactors(): TfaFactorsRepository
    fun assets(): AssetsRepository
    fun assetPairs(): AssetPairsRepository
    fun orderBook(baseAsset: String, quoteAsset: String, isBuy: Boolean): OrderBookRepository
    fun offers(onlyPrimaryMarket: Boolean = false): OffersRepository
    fun account(): AccountRepository
    fun user(): UserRepository
    fun favorites(): FavoritesRepository
    fun sales(): SalesRepository
    fun filteredSales(): SalesRepository
    fun contacts(): ContactsRepository
    fun limits(): LimitsRepository
}