package org.tokend.template.features.withdraw.amount.view

import android.os.Bundle
import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.features.amountscreen.view.AmountInputFragment
import org.tokend.template.view.balancepicker.BalancePickerBottomDialog

class WithdrawAmountFragment : AmountInputFragment() {

    override fun getAssetsToDisplay(): Collection<String> {
        return balancesRepository
                .itemsList
                .map(BalanceRecord::asset)
                .filter(AssetRecord::isWithdrawable)
                .map(AssetRecord::code)
                .sortedWith(assetComparator)
    }

    override fun getBalancePicker(): BalancePickerBottomDialog {
        return BalancePickerBottomDialog(
                requireContext(),
                amountFormatter,
                assetComparator,
                balancesRepository
        ) { balance ->
            balance.asset.isWithdrawable
        }
    }

    override fun getTitleText(): String? {
        return null
    }

    companion object {

        fun newInstance(requiredAsset: String? = null): WithdrawAmountFragment {
            val fragment = WithdrawAmountFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, requiredAsset)
            }
            return fragment
        }
    }
}