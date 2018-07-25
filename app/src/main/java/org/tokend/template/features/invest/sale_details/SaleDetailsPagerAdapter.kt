package org.tokend.template.features.invest.sale_details

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.tokend.sdk.api.models.sale.SimpleSale
import org.tokend.template.R
import org.tokend.template.features.invest.sale_details.fragments.SaleGeneralInfoFragment
import org.tokend.template.features.invest.sale_details.fragments.SaleOverviewFragment

class SaleDetailsPagerAdapter(sale: SimpleSale,
                              context: Context,
                              fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager) {
    private val pages = listOf(
            SaleOverviewFragment.newInstance(sale) to context.getString(R.string.sale_overview_title),
            SaleGeneralInfoFragment.newInstance(sale) to context.getString(R.string.sale_general_info_title)
    )

    override fun getItem(position: Int): Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}