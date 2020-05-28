package org.tokend.template.features.invest.view.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.tokend.template.R
import org.tokend.template.features.invest.model.SaleRecord

class SalePagerAdapter(sale: SaleRecord,
                       context: Context,
                       fragmentManager: androidx.fragment.app.FragmentManager
) : androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {
    private val pages = listOf(
            SaleOverviewFragment.newInstance() to context.getString(R.string.sale_overview_title),

            SaleDetailsFragment.newInstance(SaleDetailsFragment.getBundle(
                    saleAssetCode = sale.baseAsset.code)
            ) to context.getString(R.string.sale_details_title)
    )

    override fun getItem(position: Int): androidx.fragment.app.Fragment? {
        return pages.getOrNull(position)?.first
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages.getOrNull(position)?.second ?: ""
    }

    override fun getCount(): Int = pages.size
}