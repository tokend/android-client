package org.tokend.template.features.assets.buy.view.quoteasset.picker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.list_item_asset_with_amount.view.*
import org.tokend.template.R
import org.tokend.template.extensions.layoutInflater
import org.tokend.template.view.util.formatter.AmountFormatter

class AtomicSwapQuoteAssetsSpinnerAdapter(
        context: Context,
        private val amountFormatter: AmountFormatter
) : ArrayAdapter<AtomicSwapQuoteAssetSpinnerItem>(context, android.R.layout.simple_dropdown_item_1line) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getItemView(position, convertView, parent)
    }

    private fun getItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
                convertView
                        ?: context.layoutInflater.inflate(
                                R.layout.list_item_asset_with_amount, parent, false)

        val codeTextView = view.asset_code_text_view
        val amountTextView = view.amount_text_view

        val item = getItem(position)!!

        codeTextView.text = item.asset.code
        amountTextView.text = context.getString(
                R.string.template_amount_to_pay,
                amountFormatter.formatAssetAmount(
                        item.total,
                        item.asset,
                        withAssetCode = false
                )
        )

        return view
    }
}