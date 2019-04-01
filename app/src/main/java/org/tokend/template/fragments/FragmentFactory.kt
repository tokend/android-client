package org.tokend.template.fragments

import android.support.v4.app.Fragment
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.features.assets.AssetDetailsFragment
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.fees.FeesFragment
import org.tokend.template.features.invest.SalesFragment
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.features.invest.saledetails.fragments.SaleGeneralInfoFragment
import org.tokend.template.features.invest.saledetails.fragments.SaleOverviewFragment
import org.tokend.template.features.limits.LimitsFragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.settings.GeneralSettingsFragment
import org.tokend.template.features.trade.chart.view.AssetPairChartFragment
import org.tokend.template.features.trade.orderbook.view.OrderBookFragment
import org.tokend.template.features.trade.pairs.view.TradeAssetPairsFragment
import org.tokend.template.features.trade.history.view.TradeHistoryFragment
import org.tokend.template.features.trade.offers.view.OffersFragment
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.withdraw.WithdrawFragment

class FragmentFactory {

    fun getDashboardFragment(): Fragment {
        return DashboardFragment.newInstance()
    }

    fun getWalletFragment(asset: String? = null, needTabs: Boolean = true): Fragment {
        return WalletFragment.newInstance(asset, needTabs)
    }

    fun getAssetDetailsFragment(asset: AssetRecord, balanceCreation: Boolean = true): Fragment {
        return AssetDetailsFragment.newInstance(asset, balanceCreation)
    }

    fun getAssetDetailsFragment(assetCode: String, balanceCreation: Boolean = true): Fragment {
        return AssetDetailsFragment.newInstance(assetCode, balanceCreation)
    }

    fun getSettingsFragment(): Fragment {
        return GeneralSettingsFragment()
    }

    fun getOrderBookFragment(assetPair: AssetPairRecord): Fragment {
        return OrderBookFragment.newInstance(assetPair)
    }

    fun getWithdrawFragment(asset: String? = null): Fragment {
        return WithdrawFragment.newInstance(asset)
    }

    fun getSendFragment(asset: String? = null): Fragment {
        return SendFragment.newInstance(asset)
    }

    fun getLimitsFragment(): Fragment {
        return LimitsFragment()
    }

    fun getExploreFragment(): Fragment {
        return ExploreAssetsFragment()
    }

    fun getDepositFragment(): Fragment {
        return DepositFragment()
    }

    fun getSalesFragment(): Fragment {
        return SalesFragment()
    }

    fun getFeesFragment(): Fragment {
        return FeesFragment()
    }

    fun getSaleOverviewFragment(blobId: String): Fragment {
        return SaleOverviewFragment.newInstance(blobId)
    }

    fun getSaleGeneralInfoFragment(sale: SaleRecord): Fragment {
        return SaleGeneralInfoFragment.newInstance(sale)
    }

    fun getTradeAssetPairsFragment(): Fragment {
        return TradeAssetPairsFragment()
    }

    fun getAssetPairChartFragment(assetPair: AssetPairRecord): Fragment {
        return AssetPairChartFragment.newInstance(assetPair)
    }

    fun getTradeHistoryFragment(assetPair: AssetPairRecord): Fragment {
        return TradeHistoryFragment.newInstance(assetPair)
    }

    fun getOffersFragment(assetPair: AssetPairRecord): Fragment {
        return OffersFragment.newInstance(assetPair)
    }

    fun getOffersFragment(onlyPrimary: Boolean): Fragment {
        return OffersFragment.newInstance(onlyPrimary)
    }
}