package org.tokend.template.features.withdraw.amount.view

import android.os.Bundle
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.amountscreen.view.AmountInputFragment
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog

class WithdrawAmountFragment : AmountInputFragment() {
    override fun getBalancePicker(): BalancePickerBottomDialog {
        return BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                balanceComparator,
                balancesRepository
        ) { balance ->
            balance.asset.isWithdrawable
        }
    }

    override fun getTitleText(): String? {
        return null
    }

    companion object {
        fun getBundle(requiredAsset: String? = null) = Bundle().apply {
            putString(ASSET_EXTRA, requiredAsset)
        }

        fun newInstance(bundle: Bundle): WithdrawAmountFragment =
                WithdrawAmountFragment().withArguments(bundle)
    }
}