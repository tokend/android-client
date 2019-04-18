package org.tokend.template.features.invest.repository

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.data.repository.offers.OffersRepository
import org.tokend.template.features.invest.model.InvestmentlInfo
import org.tokend.template.features.invest.model.SaleRecord
import java.math.BigDecimal

/**
 * Holds investment info for specific sale
 */
class InvestmentInfoRepository(
        private val sale: SaleRecord,
        private val offersRepository: OffersRepository,
        private val salesRepository: SalesRepository
) : SimpleSingleItemRepository<InvestmentlInfo>() {
    override fun getItem(): Observable<InvestmentlInfo> {
        return Single.zip(
                getDetailedSaleIfNeeded(),
                getOffersByAsset(),
                BiFunction { detailedSale: SaleRecord, offers: Map<String, OfferRecord> ->
                    InvestmentlInfo(
                            offers,
                            detailedSale
                    )
                }
        )
                .toObservable()
    }

    private fun getOffersByAsset(): Single<Map<String, OfferRecord>> {
        return offersRepository
                .getForSale(sale.id)
                .map {
                    it.associateBy { offer ->
                        offer.quoteAssetCode
                    }
                }
    }

    private fun getDetailedSale(): Single<SaleRecord> {
        return salesRepository
                .getSingle(sale.id)
    }

    private fun getDetailedSaleIfNeeded(): Single<SaleRecord> {
        return if (sale.isAvailable)
            getDetailedSale()
        else Single.just(sale)
    }

    /**
     * @return balance available for investment in specified [asset]
     * considering pending offers
     */
    fun getAvailableBalance(asset: String,
                            balancesRepository: BalancesRepository): BigDecimal {
        val assetBalance = balancesRepository
                .itemsList
                .find { it.assetCode == asset }

        return getAvailableBalance(asset, assetBalance)
    }

    fun getAvailableBalance(asset: String,
                            balanceRecord: BalanceRecord?): BigDecimal {
        val offer = item?.offersByAsset?.get(asset)
        val locked = (offer?.quoteAmount ?: BigDecimal.ZERO).add(offer?.fee ?: BigDecimal.ZERO)

        val assetBalance = balanceRecord?.available ?: BigDecimal.ZERO

        return locked + assetBalance
    }

    /**
     * @return maximal investment amount in specified [asset]
     * considering pending offers, available balance, fees and sale limitations
     */
    fun getMaxInvestmentAmount(asset: String,
                               balancesRepository: BalancesRepository): BigDecimal {
        val investAssetDetails = item?.detailedSale?.quoteAssets?.find { it.code == asset }
        val maxByHardCap = (investAssetDetails?.hardCap ?: BigDecimal.ZERO)
                .subtract(investAssetDetails?.totalCurrentCap ?: BigDecimal.ZERO)
                .add(getExistingInvestmentAmount(asset))

        val maxByBalance = getAvailableBalance(asset, balancesRepository)

        return maxByBalance.min(maxByHardCap)
    }

    /**
     * @return amount of investment in specified [asset]
     * i.e. amount locked by the pending offer
     */
    fun getExistingInvestmentAmount(asset: String): BigDecimal {
        return item?.offersByAsset?.get(asset)?.quoteAmount ?: BigDecimal.ZERO
    }
}