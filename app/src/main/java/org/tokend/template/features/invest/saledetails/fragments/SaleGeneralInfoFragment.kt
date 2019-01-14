package org.tokend.template.features.invest.saledetails.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_details.*
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.R
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.view.InfoCard
import org.tokend.template.view.util.formatter.DateFormatter

class SaleGeneralInfoFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_details, container, false)
    }

    private lateinit var sale: SaleRecord

    override fun onInitAllowed() {
        try {
            sale = GsonFactory().getBaseGson().fromJson(
                    arguments?.getString(SALE_JSON_EXTRA),
                    SaleRecord::class.java)
        } catch (e: Exception) {
            return
        }

        displaySaleInfo()
    }

    private fun displaySaleInfo() {
        InfoCard(cards_layout)
                .addRow(getString(R.string.sale_info_start_time),
                        DateFormatter(requireContext()).formatCompact(sale.startDate))
                .addRow(getString(R.string.sale_info_close_time),
                        DateFormatter(requireContext()).formatCompact(sale.endDate))
                .addRow(getString(R.string.sale_info_hard_cap_base_asset), sale.defaultQuoteAsset)
                .addRow(getString(R.string.sale_info_soft_cap),
                        "${amountFormatter.formatAssetAmount(sale.softCap,
                                sale.defaultQuoteAsset)} ${sale.defaultQuoteAsset}")
                .addRow(getString(R.string.sale_info_hard_cap),
                        "${amountFormatter.formatAssetAmount(sale.hardCap,
                                sale.defaultQuoteAsset)} ${sale.defaultQuoteAsset}")
                .addRow(getString(R.string.sale_info_to_sell_template, sale.baseAssetCode),
                        amountFormatter.formatAssetAmount(sale.baseHardCap, sale.baseAssetCode))
    }

    companion object {
        private const val SALE_JSON_EXTRA = "asset"

        fun newInstance(sale: SaleRecord): SaleGeneralInfoFragment {
            val fragment = SaleGeneralInfoFragment()
            fragment.arguments = Bundle().apply {
                putString(SALE_JSON_EXTRA, GsonFactory().getBaseGson().toJson(sale))
            }
            return fragment
        }
    }
}