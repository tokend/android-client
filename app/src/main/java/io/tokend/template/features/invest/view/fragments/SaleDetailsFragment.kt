package io.tokend.template.features.invest.view.fragments

import android.os.Bundle
import io.tokend.template.R
import io.tokend.template.extensions.withArguments
import io.tokend.template.features.assets.details.view.AssetDetailsFragment
import io.tokend.template.features.invest.logic.InvestmentInfoHolder
import io.tokend.template.features.invest.model.SaleRecord
import io.tokend.template.view.InfoCard
import io.tokend.template.view.util.formatter.DateFormatters
import kotlinx.android.synthetic.main.fragment_asset_details.*

class SaleDetailsFragment : AssetDetailsFragment() {
    private lateinit var sale: SaleRecord

    override fun onInitAllowed() {
        super.onInitAllowed()

        sale = (requireActivity() as? InvestmentInfoHolder)
            ?.sale
            ?: throw IllegalStateException("Parent activity must hold SaleRecord")
    }

    override fun displaySummary() {
        val card = InfoCard(cards_layout)
            .setHeading(R.string.sale_summary_title, null)

        card
            .addRow(
                getString(R.string.sale_info_start_time),
                DateFormatters.compact(requireContext()).format(sale.startDate)
            )
            .addRow(
                getString(R.string.sale_info_close_time),
                DateFormatters.compact(requireContext()).format(sale.endDate)
            )
            .addRow(
                getString(R.string.sale_info_soft_cap),
                amountFormatter.formatAssetAmount(sale.softCap, sale.defaultQuoteAsset)
            )
            .addRow(
                getString(R.string.sale_info_hard_cap),
                amountFormatter.formatAssetAmount(sale.hardCap, sale.defaultQuoteAsset)
            )
            .addRow(
                getString(R.string.sale_info_to_sell_template, sale.baseAsset.code),
                amountFormatter.formatAssetAmount(
                    sale.baseHardCap, sale.baseAsset,
                    withAssetCode = false
                )
            )

        super.displaySummary()
    }

    companion object {
        fun newInstance(bundle: Bundle): SaleDetailsFragment =
            SaleDetailsFragment().withArguments(bundle)

        fun getBundle(saleAssetCode: String) = getBundle(saleAssetCode, false)
    }
}