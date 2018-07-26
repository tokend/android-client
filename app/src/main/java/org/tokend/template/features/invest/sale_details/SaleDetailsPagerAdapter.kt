package org.tokend.template.features.invest.sale_details

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.tokend.sdk.api.models.sale.SimpleSale
import org.tokend.template.R
import org.tokend.template.util.FragmentFactory

class SaleDetailsPagerAdapter(sale: SimpleSale,
                              context: Context,
                              fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {
    private val fragmentFactory = FragmentFactory()

    private val pages = listOf(
            fragmentFactory.getSaleOverviewFragment(sale.details.descriptionBlob) to
                    context.getString(R.string.sale_overview_title),
            fragmentFactory.getSaleGeneralInfoFragment(sale) to
                    context.getString(R.string.sale_general_info_title),
            fragmentFactory.getAssetDetailsFragment(sale.baseAsset, balanceCreation = false) to
                    context.getString(R.string.sale_token_title)
    )

    override fun getItem(position: Int): Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}