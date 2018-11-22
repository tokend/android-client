package org.tokend.template.fragments

import android.support.v4.app.Fragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.settings.GeneralSettingsFragment
import org.tokend.template.extensions.Asset
import org.tokend.template.extensions.Sale
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.assets.AssetDetailsFragment
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.invest.SalesFragment
import org.tokend.template.features.invest.saledetails.fragments.SaleGeneralInfoFragment
import org.tokend.template.features.invest.saledetails.fragments.SaleOverviewFragment
import org.tokend.template.features.limits.LimitsFragment
import org.tokend.template.features.trade.TradeFragment
import org.tokend.template.features.withdraw.WithdrawFragment

class FragmentFactory {

    fun getDashboardFragment(): Fragment {
        return DashboardFragment.newInstance()
    }

    fun getWalletFragment(asset: String? = null, needTabs: Boolean = true): Fragment {
        return WalletFragment.newInstance(asset, needTabs)
    }

    fun getAssetDetailsFragment(asset: Asset, balanceCreation: Boolean = true): Fragment {
        return AssetDetailsFragment.newInstance(asset, balanceCreation)
    }

    fun getAssetDetailsFragment(assetCode: String, balanceCreation: Boolean = true): Fragment {
        return AssetDetailsFragment.newInstance(assetCode, balanceCreation)
    }

    fun getSettingsFragment(): Fragment {
        return GeneralSettingsFragment()
    }

    fun getTradeFragment(): Fragment {
        return TradeFragment()
    }

    fun getWithdrawFragment(asset: String? = null): Fragment {
        return WithdrawFragment.newInstance(asset)
    }

    fun getSendFragment(asset: String? = null): Fragment {
        return SendFragment.newInstance(asset)
    }

    fun getLimitsFragment(): Fragment {
        return LimitsFragment.newInstance()
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

    fun getSaleOverviewFragment(blobId: String): Fragment {
        return SaleOverviewFragment.newInstance(blobId)
    }

    fun getSaleGeneralInfoFragment(sale: Sale): Fragment {
        return SaleGeneralInfoFragment.newInstance(sale)
    }
}