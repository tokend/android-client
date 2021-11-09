package io.tokend.template.features.deposit.view

import android.os.Bundle
import io.tokend.template.R
import io.tokend.template.extensions.withArguments
import io.tokend.template.features.amountscreen.view.AmountInputFragment
import kotlinx.android.synthetic.main.fragment_amount_input.*

class DepositAmountFragment : AmountInputFragment() {
    override fun initSelection() {
        asset_code_text_view.background = null
    }

    override fun checkAmount() {}

    override fun getTitleText(): String? =
        getString(R.string.enter_deposit_amount)

    companion object {
        fun getBundle(assetCode: String) = getBundle(requiredAssetCode = assetCode)

        fun newInstance(bundle: Bundle): DepositAmountFragment =
            DepositAmountFragment().withArguments(bundle)
    }
}