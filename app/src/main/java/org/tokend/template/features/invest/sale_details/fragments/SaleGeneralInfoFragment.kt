package org.tokend.template.features.invest.sale_details.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.view.InfoCard
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.extensions.Sale
import org.tokend.template.util.DateFormatter

class SaleGeneralInfoFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_details, container, false)
    }

    private lateinit var sale: Sale

    override fun onInitAllowed() {
        try {
            sale = GsonFactory().getBaseGson().fromJson(
                    arguments?.getString(SALE_JSON_EXTRA),
                    Sale::class.java)
        } catch (e: Exception) {
            return
        }

        displaySaleInfo()
    }

    private fun displaySaleInfo() {
        InfoCard(cards_layout)
                .addRow("Start time",
                        DateFormatter(requireContext()).formatCompact(sale.startDate))
                .addRow("Close time",
                        DateFormatter(requireContext()).formatCompact(sale.endDate))
                .addRow("Base asset for hard cap", sale.defaultQuoteAsset)
                .addRow("Soft cap",
                        "${AmountFormatter.formatAssetAmount(sale.softCap,
                                sale.defaultQuoteAsset)} ${sale.defaultQuoteAsset}")
                .addRow("Hard cap",
                        "${AmountFormatter.formatAssetAmount(sale.hardCap,
                                sale.defaultQuoteAsset)} ${sale.defaultQuoteAsset}")
                .addRow("${sale.baseAsset} to sell",
                        AmountFormatter.formatAssetAmount(sale.baseHardCap, sale.baseAsset))
    }

    companion object {
        private const val SALE_JSON_EXTRA = "asset"

        fun newInstance(sale: Sale): SaleGeneralInfoFragment {
            val fragment = SaleGeneralInfoFragment()
            fragment.arguments = Bundle().apply {
                putString(SALE_JSON_EXTRA, GsonFactory().getBaseGson().toJson(sale))
            }
            return fragment
        }
    }
}