package org.tokend.template.di.providers

import org.tokend.template.data.repository.*
import org.tokend.template.data.repository.assets.AssetChartRepository
import org.tokend.template.data.repository.assets.AssetsRepository
import org.tokend.template.data.repository.balancechanges.BalanceChangesRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.pairs.AssetPairsRepository
import org.tokend.template.data.repository.tfa.TfaFactorsRepository
import org.tokend.template.data.repository.tradehistory.TradeHistoryRepository
import org.tokend.template.features.invest.repository.SalesRepository
import org.tokend.template.features.kyc.storage.KycStateRepository
import org.tokend.template.features.offers.repository.OffersRepository
import org.tokend.template.features.send.recipient.repository.ContactsRepository
import org.tokend.template.features.trade.orderbook.repository.OrderBookRepository

interface RepositoryProvider {
    fun balances(): BalancesRepository
    fun accountDetails(): AccountDetailsRepository
    fun systemInfo(): SystemInfoRepository
    fun tfaFactors(): TfaFactorsRepository
    fun assets(): AssetsRepository
    fun assetPairs(): AssetPairsRepository
    fun orderBook(baseAsset: String, quoteAsset: String): OrderBookRepository
    fun offers(onlyPrimaryMarket: Boolean = false): OffersRepository
    fun account(): AccountRepository
    fun sales(): SalesRepository
    fun filteredSales(): SalesRepository
    fun contacts(): ContactsRepository
    fun limits(): LimitsRepository
    fun fees(): FeesRepository
    fun balanceChanges(balanceId: String): BalanceChangesRepository
    fun tradeHistory(base: String, quote: String): TradeHistoryRepository
    fun assetChart(asset: String): AssetChartRepository
    fun assetChart(baseAsset: String, quoteAsset: String): AssetChartRepository
    fun kycState(): KycStateRepository
}