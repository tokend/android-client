package io.tokend.template.features.withdraw.amount.view

import android.os.Bundle
import io.tokend.template.extensions.withArguments
import io.tokend.template.features.amountscreen.view.AmountInputFragment
import io.tokend.template.view.balancepicker.BalancePickerBottomDialog

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
        fun getBundle(requiredBalanceId: String? = null) =
            AmountInputFragment.getBundle(requiredBalanceId = requiredBalanceId)

        fun newInstance(bundle: Bundle): WithdrawAmountFragment =
            WithdrawAmountFragment().withArguments(bundle)
    }
}