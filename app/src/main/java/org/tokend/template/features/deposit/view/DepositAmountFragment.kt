package org.tokend.template.features.deposit.view

import android.os.Bundle
import kotlinx.android.synthetic.main.fragment_amount_input.*
import org.tokend.template.R
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.amountscreen.view.AmountInputFragment

class DepositAmountFragment : AmountInputFragment() {
    override fun initAssetSelection() {
        asset_code_text_view.background = null
    }

    override fun checkAmount() {}

    override fun getTitleText(): String? =
            getString(R.string.enter_deposit_amount)

    companion object {
        fun getBundle(assetCode: String) = Bundle().apply {
            putString(ASSET_EXTRA, assetCode)
        }

        fun newInstance(bundle: Bundle): DepositAmountFragment =
                DepositAmountFragment().withArguments(bundle)
    }
}